import { PluginInitializerContext } from '../../../src/core/server';
import { CodePlugin } from './plugin';

//  This exports static code and TypeScript types,
//  as well as, Kibana Platform `plugin()` initializer.

export function plugin(initializerContext: PluginInitializerContext) {
  return new CodePlugin(initializerContext);
}

export { CodePluginSetup, CodePluginStart } from './types';
