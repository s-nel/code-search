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
import { ConnectorSelector } from 'x-pack/plugins/cases/public/components/connector_selector/form';

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
  const [isEmpty, setIsEmpty] = useState<boolean>(false);
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
      loadLogs([res])
    }).finally(() => {
      setIsLoading(false)
      setIsEmpty(false)
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
      loadLogs(fs)
    }).finally(() => {
      setIsLoading(false)
      setIsEmpty(false)
    });
  }

  function componentToHex(c) {
    var hex = c.toString(16);
    return hex.length == 1 ? "0" + hex : hex;
  }
  
  function rgbToHex(r, g, b) {
    return "#" + componentToHex(r) + componentToHex(g) + componentToHex(b);
  }

  const loadLogs = (files) => {
    if (files) {
      const onlyUnique = (value, index, self) => {
        return self.indexOf(value) === index
      }

      const classStr = files.flatMap(f => {
        return f.spans.filter(s => {
          return s.element.kind == 'class' || s.element.kind == 'object'
        }).flatMap(s => s.element.name)
      }).filter(onlyUnique)

      http.get('/api/code/logs', {
        query: {
          classes: classStr.join(',')
        }
      }).then((res) => {
        const stackTraces = res.hits.hits.map(h => h['_source']).flatMap(s => s && s.error && s.error.stack_trace ? [s.error.stack_trace] : [])
        files.forEach(f => {
          const problemLines = stackTraces.flatMap(st => {
            const stls = st.split("\n")
            return stls.flatMap((stl, index) => {
              const regexpStr = `\\(${f.file_name}:(\\d+)\\)`
              const match = stl.match(new RegExp(regexpStr))
              if (match) {
                return [{
                  line: parseInt(match[1]),
                  weight: Math.pow(stls.length - index, 6)
                }]
              } else {
                return []
              }
            })
          })

          f.lineProblems = {}
          var maxLineProblems = 0
          problemLines.forEach(l => {
            f.lineProblems[l.line] = f.lineProblems[l.line] ? f.lineProblems[l.line] + l.weight : l.weight;
            if (f.lineProblems[l.line] > maxLineProblems) {
              maxLineProblems = f.lineProblems[l.line]
            }
          })

          Object.keys(f.lineProblems).map(k => {
            const fraction = f.lineProblems[k] / maxLineProblems
            console.log(f.lineProblems[k], fraction, maxLineProblems)
            const r = 255
            const g = Math.round((1 - fraction) * 140)
            const b = 0
            const a = fraction * .4
            const color = rgbToHex(r, g, b)
            console.log('color', r, g, b, color)

            f.lineProblems[k] = {
              count: f.lineProblems[k],
              color: {r, g, b, a},
            }
          })
        })
        console.log(files)
        setFiles(files)
      }).catch(e => {
        setFiles(files)
      })
    }
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

          setOutline(outline)
        }
        setOutlineLoading(false)
    })
  }

  if (!loadedFromParams && (!!params.class || !!params.file)) {
    setLoadedFromParams(true)
    findClass(params.class, params.file)
  } else if (!loadedFromParams && (!params.class && !params.file)) {
    setLoadedFromParams(true)
    setIsEmpty(true)
  }

  if (outline == null && !outlineLoading) {
    loadOutline()
  }

  const lineProblemHighlightClasses = (file) => {
    console.log(file.lineProblems)
    const result = file.lineProblems && Object.keys(file.lineProblems).map(k => {
      const lp = file.lineProblems[k]
      return `span.euiCodeBlock__line:nth-of-type(${k}) { background-color: rgba(${lp.color.r}, ${lp.color.g}, ${lp.color.b}, ${lp.color.a}); }`
    }).join("\n")
    console.log(result)
    return result
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
                { !files && !isLoading && !isEmpty && <EuiEmptyPrompt color="subdued" title={<h3>Class not found</h3>} layout="vertical"/> }
                {
                  !isLoading && files && files.map(file => (
                    <div>
                      <style>{lineProblemHighlightClasses(file)}</style>
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
