import { History } from 'history';
import React, { useState } from 'react';
import { i18n } from '@kbn/i18n';
import { FormattedMessage, I18nProvider } from '@kbn/i18n/react';
import { Router } from 'react-router-dom';
import { parse, ParsedQuery } from 'query-string';

import {
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
  EuiTitle,
  EuiText,
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
  // Use React hooks to manage state.
  const [file, setFile] = useState<object | undefined>();
  const [notFound, setNotFound] = useState<boolean>(false);

  const findClass = (cls, file) => {
    // Use the core http service to make a response to the server API.
    http.get('/api/code/class', {
      query: {
        class: cls,
        file: file,
      },
    }).then((res) => {
      console.log(res);
      setFile(res);
      // Use the core notifications service to display a success message.
      notifications.toasts.addSuccess(
        i18n.translate('code.dataUpdated', {
          defaultMessage: 'Data updated',
        })
      );
    }).catch(() => {
      setNotFound(true)
    });
  };

  const params = parse(history.location.search)

  if (!file && !notFound) {
    findClass(params.class, params.file)
  }

  const lineNumbers = params.lines ? {
    highlight: params.lines
  } : true

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
          <EuiPage>
            <EuiPageBody>
              <EuiPageHeader>
                <EuiTitle size="l">
                  <h1>
                    <FormattedMessage
                      id="code.helloWorldText"
                      defaultMessage="{name}"
                      values={{ name: PLUGIN_NAME }}
                    />
                  </h1>
                </EuiTitle>
              </EuiPageHeader>
              <EuiPageContent>
                <EuiPageContentHeader>
                  <EuiTitle>
                    <h2>
                      <pre>
                        {file?.file_name}
                      </pre>
                    </h2>
                  </EuiTitle>
                </EuiPageContentHeader>
                  <EuiPageContentBody>
                    { notFound ? (
                        <p>File not found</p>
                    ): (!file ? (
                        <EuiLoadingContent/>
                    ) : (

                          <EuiText>
                            <EuiCodeBlock language={file?.language?.name.toLowerCase()} lineNumbers={lineNumbers}>
                              {file?.source?.content}
                            </EuiCodeBlock>
                          </EuiText>
                    ))}
                </EuiPageContentBody>
              </EuiPageContent>
            </EuiPageBody>
          </EuiPage>
        </>
      </I18nProvider>
    </Router>
  );
};
