import { RefObject } from 'react';
import { useScrollbarWidth } from 'react-use';

import { Nullable } from 'types/util';

export const useElementScrollbarWidth = (
  elementRef: RefObject<Nullable<HTMLElement>>
) => {
  const browserScrollbarWidth = useScrollbarWidth() ?? 0;

  if (!elementRef.current) {
    return 0;
  }

  const elementHasScrollBar =
    elementRef.current.scrollHeight > elementRef.current.clientHeight;

  return elementHasScrollBar ? browserScrollbarWidth : 0;
};
