import './index.scss';

import { CodePlugin } from './plugin';

// This exports static code and TypeScript types,
// as well as, Kibana Platform `plugin()` initializer.
export function plugin() {
  return new CodePlugin();
}
export { CodePluginSetup, CodePluginStart } from './types';
