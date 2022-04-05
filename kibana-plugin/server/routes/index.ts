import { IRouter } from '../../../../src/core/server';
import type { estypes } from '@elastic/elasticsearch';

export function defineRoutes(router: IRouter) {
  router.get(
    {
      path: '/api/code/class',
      validate: false,
    },
    async (context, request, response) => {
      const requestClient = context.core.elasticsearch.client.asCurrentUser;

      console.log(request)

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

      console.log(search)
      
      const firstHit = result?.body?.hits?.hits?.[0]?.['_source']

      if (!firstHit) {
        return response.notFound()
      }

      return response.ok({
        body: firstHit,
      });
    }
  );
}