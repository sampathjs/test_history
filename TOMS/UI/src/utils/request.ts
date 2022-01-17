import isEmpty from 'lodash/isEmpty';

import { REACT_APP_API_URL, REACT_APP_MOCK_API } from 'config';

type RequestMethodType = 'POST' | 'GET' | 'PUT';
type RequestOptionsType = {
  body?:
    | Blob
    | BufferSource
    | FormData
    | URLSearchParams
    | ReadableStream<Uint8Array>
    | string
    | null;
  headers?: Record<string, string>;
  redirect?: RequestRedirect;
  credentials?: RequestCredentials;
  method?: RequestMethodType;
};

const HOST = REACT_APP_MOCK_API ? '/toms' : REACT_APP_API_URL;

export const buildUrl = (path: string, params?: string) =>
  `${path}${!isEmpty(params) ? `?${params}` : ''}`;

const isJsonResponse = (response: Response) =>
  response.headers.get('content-type')?.includes('application/json');

export const request = async <T>(url: string, options?: RequestOptionsType) => {
  try {
    const res = await fetch(`${HOST}/${url}`, {
      ...options,
      headers: {
        Accept: 'application/json',
        'Content-Type': 'application/json',
        ...options?.headers,
      },
    });

    if (!res.ok) {
      throw new Error('Network response was not ok.');
    }

    if (!isJsonResponse(res)) {
      return undefined as unknown as T;
    }

    const data = (await res.json()) as T;

    return data;
  } catch (error) {
    throw new Error(
      error instanceof Error ? error.message : 'Unknown server error'
    );
  }
};
