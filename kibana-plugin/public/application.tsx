import React from 'react';
import ReactDOM from 'react-dom';
import { RouteRenderer, RouterProvider } from '@kbn/typed-react-router-config';
import { AppMountParameters, CoreStart } from '../../../src/core/public';
import { AppPluginStartDependencies } from './types';
import { CodeApp } from './components/app';
import { CodePluginSetupDeps } from './plugin';
import {
  KibanaContextProvider,
} from '../../../src/plugins/kibana_react/public';

export const renderApp = (
  core: CoreStart,
  startDeps: CodePluginStartDeps,
  setupDeps: CodePluginSetupDeps,
  { appBasePath, element, history }: AppMountParameters
) => {
  const { observability, navigation } = startDeps;
  const { notifications, http } = core
  console.log(observability)
  console.log(observability.navigation)
  console.log(observability.navigation.PageTemplate)
  const ObservabilityPageTemplate = observability.navigation.PageTemplate;
  ReactDOM.render((
    <KibanaContextProvider services={{ ...core, ...startDeps }}>
      <RouterProvider history={history}>
        <ObservabilityPageTemplate
          pageHeader={{
            pageTitle: 'Code'
          }}
        >
          <CodeApp
            basename={appBasePath}
            notifications={notifications}
            http={http}
            navigation={navigation}
            history={history}
          />
        </ObservabilityPageTemplate>
      </RouterProvider>
    </KibanaContextProvider>),
    element
  );

  return () => ReactDOM.unmountComponentAtNode(element);
};
