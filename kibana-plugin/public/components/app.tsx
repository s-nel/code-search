import { History } from 'history';
import React, { useState } from 'react';
import { i18n } from '@kbn/i18n';
import { FormattedMessage, I18nProvider } from '@kbn/i18n/react';
import { Router } from 'react-router-dom';
import { parse, ParsedQuery } from 'query-string';

import {
  htmlIdGenerator,
  EuiButton,
  EuiCodeBlock,
  EuiHorizontalRule,
  EuiLoadingContent,
  EuiPage,
  EuiPageBody,
  EuiPageContent,
  EuiPageContentBody,
  EuiPageContentHeader,
  EuiPageHeader,
  EuiFieldSearch,
  EuiTreeView,
  EuiSpacer,
  EuiTitle,
  EuiText,
  EuiFlexGroup,
  EuiFlexItem,
  EuiToken,
  EuiTreeViewNode,
  EuiSpinner,
  EuiPanel,
  EuiEmptyPrompt
} from '@elastic/eui';

import { CoreStart, ScopedHistory } from '../../../../src/core/public';
import { NavigationPublicPluginStart } from '../../../../src/plugins/navigation/public';

import { PLUGIN_ID, PLUGIN_NAME } from '../../common';
import { SerializableRecord } from '@kbn/utility-types';
import { integer } from '@elastic/elasticsearch/api/types';

interface CodeAppDeps {
  basename: string;
  notifications: CoreStart['notifications'];
  http: CoreStart['http'];
  navigation: NavigationPublicPluginStart;
  history: ScopedHistory<unknown>;
}

export const CodeApp = ({ basename, notifications, http, navigation, history }: CodeAppDeps) => {
  const params = parse(history.location.search)

  // Use React hooks to manage state.
  const [loadedFromParams, setLoadedFromParams] = useState<boolean>(false);
  const [files, setFiles] = useState<object[] | undefined>(undefined);
  const [isLoading, setIsLoading] = useState<boolean>(false)
  const [isSearching, setSearching] = useState<boolean>(false)
  const [query, setQuery] = useState('')
  const [lines, setLines] = useState(params.lines ? {
                                         highlight: params.lines
                                       } : true)
  const [outline, setOutline] = useState(null)    
  const [outlineLoading, setOutlineLoading] = useState(false);

  function findClass(cls: string, file: string) {
    setIsLoading(true)
    setFiles(undefined)
    // Use the core http service to make a response to the server API.
    http.get('/api/code/class', {
      query: {
        class: cls,
        file: file,
      },
    }).then((res) => {
      setFiles([res]);
      setIsLoading(false)
    });
  }

  const onSearchChange = (e) => {
    const q = e.target.value
    console.log(q)
    setQuery(q)
  }

  function search() {
    setIsLoading(true)
    setFiles(undefined)
    setSearching(true)
    setLines(true)
    http.get('/api/code/search', {
      query: {
          q: query
      }
    }).then((res) => {
      setSearching(false)
      console.log(res);
          
      const fs = res.hits.hits.map(hit => hit['_source'])
      setFiles(fs)
      setIsLoading(false)
    })
  }

  const loadOutline = () => {
    setOutlineLoading(true)
    http.get('/api/code/search', {
      query: {
          q: ''
      }
    }).then((res) => {
        if (res?.hits?.hits?.length > 0) {
          const classes = res.hits.hits.flatMap(hit => {
            const src = hit['_source']
            return src.spans.filter(span => span.element.kind == 'class')
          })

          console.log(classes)

          var outline: EuiTreeViewNode[] = []

          const findOrCreatePackage = (pkg: string[], span, o: EuiTreeViewNode[]): EuiTreeViewNode => {
            const foundPkg = o.find(p => p.id == pkg[0])
            if (foundPkg) {
              if (pkg.length <= 1) {
                return foundPkg
              } else {
                if (!foundPkg.children) {
                  foundPkg.children = []
                }
                return findOrCreatePackage(pkg.slice(1), span, foundPkg.children)
              }
            } else {
              const isCls = pkg[0].toUpperCase().charAt(0) == pkg[0].charAt(0)
              const newPkg: EuiTreeViewNode = {
                label: pkg[0],
                id: pkg[0],
                icon: isCls ? <EuiToken iconType="tokenClass" /> : <EuiToken iconType="tokenPackage" />,
                callback: () => {
                  if (isCls) {
                    setLines(true)
                    findClass(span.element.name[0], undefined)
                  }
                }
              }
              o.push(newPkg)
              if (pkg.length <= 1) {
                return newPkg
              } else {
                newPkg.children = []
                return findOrCreatePackage(pkg.slice(1), span, newPkg.children)
              }
            }
          }

          classes.forEach(cls => {
            const packageArr = cls.element.name[0].split('.')
            findOrCreatePackage(packageArr, cls, outline)
          })

          console.log(outline)

          setOutline(outline)
        }
        setOutlineLoading(false)
    })
  }

  if (!loadedFromParams && (!!params.class || !!params.file)) {
    setLoadedFromParams(true)
    findClass(params.class, params.file)
  }

  if (outline == null && !outlineLoading) {
    loadOutline()
  }

  // Render the application DOM.
  // Note that `navigation.ui.TopNavMenu` is a stateful component exported on the `navigation` plugin's start contract.
  return (
      <Router history={history}>
        <I18nProvider>
          <>
            <navigation.ui.TopNavMenu
              appName={PLUGIN_ID}
              showSearchBar={false}
              useDefaultBehaviors={true}
            />
            <EuiFieldSearch value={query} fullWidth onChange={onSearchChange} onSearch={search} isLoading={isSearching} suggestions={[]} placeholder="Search code" status={status} />
            <EuiSpacer />
            <EuiFlexGroup>
              <EuiFlexItem>
                <EuiPanel grow={false} style={{minWidth: 200}}>
                  {outline && (<EuiTreeView 
                    display="compressed"
                    expandByDefault
                    showExpansionArrows 
                    items={outline} 
                  />)}
                </EuiPanel>
              </EuiFlexItem>
              <EuiFlexItem grow={8}>
                { isLoading && <EuiLoadingContent lines={3} /> }
                { !files && !isLoading && <EuiEmptyPrompt color="subdued" title={<h3>Class not found</h3>} layout="vertical"/> }
                {
                  !isLoading && files && files.map(file => (
                    <div>
                      <EuiPageContentHeader>
                        <EuiTitle size="s">
                            <pre>
                              {file?.file_name}
                            </pre>
                        </EuiTitle>
                      </EuiPageContentHeader>
                      <EuiSpacer />
                        <EuiPageContentBody>

                            <EuiText>
                              <EuiCodeBlock language={file?.language?.name.toLowerCase()} lineNumbers={lines}>
                                {file?.source?.content}
                              </EuiCodeBlock>
                            </EuiText>
                      </EuiPageContentBody>
                      <EuiSpacer />
                    </div>
                  ))
                }
              </EuiFlexItem>
            </EuiFlexGroup>
          </>
        </I18nProvider>
      </Router>
  );
};
