import { IRouter } from '../../../../src/core/server';
import type { estypes } from '@elastic/elasticsearch';

export function defineRoutes(router: IRouter) {
  router.get(
    {
      path: '/api/code/search',
      validate: false,
    },
    async (context, request, response) => {
        const requestClient = context.core.elasticsearch.client.asCurrentUser;
        const q = request.url.searchParams.get('q')

        const result = await requestClient.search({
            index: 'code-search-*',
            body: {
                query: {
                    bool: {
                        should: [
                          {
                            nested: {
                                path: "spans",
                                query: {
                                    prefix: {
                                        "spans.element.name": {
                                            value: q
                                        }
                                    }
                                }
                            }
                          }
                        ]
                    }
                }
            }
        });

        return response.ok({
          body: result?.body,
        });
    }
  )

  router.get(
    {
      path: '/api/code/class',
      validate: false,
    },
    async (context, request, response) => {
      const requestClient = context.core.elasticsearch.client.asCurrentUser;

      var filters: estypes.QueryDslQueryContainer[] = []
      if (request.url.searchParams.get('class')) {
        filters.push({
          nested: {
            path: "spans",
            query: {
              term: {
                "spans.element.name": request.url.searchParams.get('class')
              }
            }
          }
        })
      }
      if (request.url.searchParams.get('file')) {
        filters.push({
          term: {
            file_name: request.url.searchParams.get('file')
          }
        })
      }

      const search = {
        query: {
          bool: {
            filter: filters
          }
        }
      }
      
      const result = await requestClient.search({
        index: 'code-search-*',
        body: search
      });
      
      const firstHit = result?.body?.hits?.hits?.[0]?.['_source']

      if (!firstHit) {
        return response.notFound()
      }

      return response.ok({
        body: firstHit,
      });
    }
  );

  router.get(
    {
      path: '/api/code/logs',
      validate: false,
    },
    async (context, request, response) => {
      const requestClient = context.core.elasticsearch.client.asCurrentUser;
      const classes = request.url.searchParams.get('classes')?.split(',')
      
      const shoulds = classes.map(c => {
        return {
          regexp: {
            'error.stack_trace': {
              value: `.*\s${c}\..*`
            }
          }
        }
      })

      const result = await requestClient.search({
        index: 'filebeat-*',
        body: {
          query: {
            bool: {
              should: shoulds,
              filter: [
                {
                  range: {
                    '@timestamp': {
                      format: 'strict_date_optional_time',
                      gte: 'now-30d'
                    }
                  }
                }
              ]
            }
          }
        }
      })

      return response.ok({
        body: result?.body,
      })
    }
  )
}
