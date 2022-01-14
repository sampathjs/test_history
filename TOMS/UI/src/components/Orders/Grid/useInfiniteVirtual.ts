import { RefObject, useCallback, useEffect, useState } from 'react';
import useEvent from 'react-use/lib/useEvent';
import { useVirtual } from 'react-virtual';

import { Nullable } from 'types/util';

const OFFSET_THRESHOLD_TO_LOAD_MORE = 50;

type Props = Parameters<typeof useVirtual>[0] & {
  parentRef: RefObject<Nullable<HTMLElement>>;
};

const getScrollOffsetFromBottom = (element: HTMLElement) => {
  const { offsetHeight, scrollHeight, scrollTop } = element;

  return scrollHeight - (offsetHeight + scrollTop);
};

export const useInfiniteVirtual = (props: Props) => {
  const virtual = useVirtual(props);
  const { virtualItems } = virtual;
  const { parentRef } = props;
  const [shouldLoadMore, setShouldLoadMore] = useState(false);

  const checkIfShouldLoadMore = useCallback(() => {
    if (!parentRef.current) {
      return;
    }

    setShouldLoadMore(
      getScrollOffsetFromBottom(parentRef.current) <
        OFFSET_THRESHOLD_TO_LOAD_MORE
    );
  }, [parentRef]);

  useEvent('scroll', checkIfShouldLoadMore, parentRef.current);

  useEffect(() => {
    checkIfShouldLoadMore();
  }, [checkIfShouldLoadMore, virtualItems]);

  return { ...virtual, shouldLoadMore };
};
