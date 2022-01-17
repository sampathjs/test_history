import {
  createContext,
  ReactNode,
  useCallback,
  useContext,
  useState,
} from 'react';

import { Nullable } from 'types/util';

import { OrderPanelView } from './types';

type ContextProps = {
  view?: OrderPanelView;
  setView(view: OrderPanelView): void;
  clearView(): void;
};

const OrderPanelContext = createContext<Nullable<ContextProps>>(null);

type Props = {
  children: ReactNode;
};

export const OrderPanelProvider = (props: Props) => {
  const { children } = props;
  const [view, setView] = useState<OrderPanelView | undefined>();

  const clearView = useCallback(() => {
    setView(undefined);
  }, []);

  return (
    <OrderPanelContext.Provider value={{ view, setView, clearView }}>
      {children}
    </OrderPanelContext.Provider>
  );
};

export const useOrderPanelContext = () => {
  const value = useContext(OrderPanelContext);

  if (!value) {
    throw new Error(
      'useOrderPanelContext must be used within OrderPanelProvider'
    );
  }

  return value;
};
