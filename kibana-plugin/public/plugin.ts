import { i18n } from '@kbn/i18n';
import { from } from 'rxjs';
import { map } from 'rxjs/operators';
import { AppMountParameters, CoreSetup, CoreStart, Plugin } from '../../../src/core/public';
import { CodePluginSetup, CodePluginStart, AppPluginStartDependencies } from './types';
import { PLUGIN_NAME } from '../common';
import { FieldFormatsSetup } from 'src/plugins/field_formats/public';
import { KBN_FIELD_TYPES } from '@kbn/field-types';
import { FieldFormat } from '../../../src/plugins/field_formats/common';
import {
  FetchDataParams,
  HasDataParams,
  METRIC_TYPE,
  ObservabilityPublicSetup,
  ObservabilityPublicStart,
} from '../../../x-pack/plugins/observability/public';

export interface CodePluginSetupDeps {
  fieldFormats: FieldFormatsSetup;
}

export interface CodePluginStartDeps {
  observability: ObservabilityPublicStart;
  navigation: NavigationPublicStart;
}

export class CodePlugin implements Plugin<CodePluginSetup, CodePluginStart, CodePluginSetupDeps> {
  public setup(core: CoreSetup, setupDeps: CodePluginSetupDeps): CodePluginSetup {
    class StackTraceFormat extends FieldFormat {
      static id = 'stack-trace';
      static title = 'Java Stack Trace';

      static fieldType = KBN_FIELD_TYPES.STRING;

      htmlConvert = (val: unknown, options?: HtmlContextTypeOptions) => {
        if (typeof val !== 'string') {
          return `${val}`;
        }

        const codeUrl = (cls, file, lineNum) => {
            return core.http.basePath.prepend(`/app/code?class=${cls}&file=${file}&lines=${lineNum}`);
        }

        const lines = val.split(/\n/)
        const formattedLines = []
        lines.forEach(line => {
            if (line.trim().startsWith("at")) {
                const matches = line.match(/(\s*at )([^\(]+)\(([^:]+):(\d+)\)/);
                if (matches && matches.length == 5) {
                    console.log(matches);
                    const clsMethod = matches[2];
                    const cls = clsMethod.substring(0, clsMethod.lastIndexOf("."))
                    const file = matches[3];
                    const lineNum = matches[4];
                    if (cls.startsWith("foo") || cls.startsWith("bar")) {
                      const url = codeUrl(cls, file, lineNum)
                      formattedLines.push(`${matches[1]}${clsMethod}(<a href="${url}">${file}:${lineNum}</a>)`);
                    } else {
                      formattedLines.push(line)
                    }
                } else {
                    formattedLines.push(line)
                }
            } else {
                formattedLines.push(line)
            }
        })
    
        //return `<a href="${codeUrl}">${val}</a>`;
        return formattedLines.join("\n");
      };
    }

    class LoggerFormat extends FieldFormat {
      static id = 'logger';
      static title = 'Logger Classname';

      static fieldType = KBN_FIELD_TYPES.STRING;

      htmlConvert = (val: unknown, options?: HtmlContextTypeOptions) => {
        if (typeof val !== 'string') {
          return `${val}`;
        }

        const codeUrl = (cls) => {
            return core.http.basePath.prepend(`/app/code?class=${cls}`);
        }
    
        return `<a href="${codeUrl(val)}">${val}</a>`;
      };
    }

    setupDeps.fieldFormats.register([StackTraceFormat])
    setupDeps.fieldFormats.register([LoggerFormat])

    // register observability nav if user has access to plugin
    setupDeps.observability.navigation.registerSections(
      from(core.getStartServices()).pipe(
        map(([coreStart, pluginsStart]) => {
          return [
            // Code navigation
            {
              label: 'Code',
              sortKey: 220,
              entries: [
                { label: 'Browser', app: 'code', path: '' },
              ],
            },
          ];

          return [];
        })
      )
    );

    // Register an application into the side navigation menu
    core.application.register({
      id: 'code',
      title: "Code",
      order: 8150,
      category: {
        id: 'observability',
        label: i18n.translate('core.ui.observabilityNavList.label', {
          defaultMessage: 'Observability',
        }),
        euiIconType: 'logoObservability',
        order: 3000,
      },
      async mount(params: AppMountParameters) {
        // Load application bundle
        const [{ renderApp }, [coreStart, depsStart]] = await Promise.all([
            import('./application'),
            core.getStartServices()
        ]);
        // Render the application
        return renderApp(coreStart, depsStart as CodePluginStartDeps, setupDeps, params);
      },
    });

    // Return methods that should be available to other plugins
    return {
      getGreeting() {
        return i18n.translate('code.greetingText', {
          defaultMessage: 'Hello from {name}!',
          values: {
            name: PLUGIN_NAME,
          },
        });
      },
    };
  }

  public start(core: CoreStart): CodePluginStart {
    return {};
  }

  public stop() {}
}
