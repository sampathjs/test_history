import {
  createContext,
  ReactNode,
  useContext,
  useEffect,
  useState,
} from 'react';
import { useForm, UseFormReturn } from 'react-hook-form';

import { useAuthContext } from 'contexts';
import { useCreateFills } from 'hooks/useCreateFills';
import { useOrderFills } from 'hooks/useOrderFills';
import { Order } from 'types';
import { Nullable } from 'types/util';

import * as Styles from '../OrderPanel.styles';
import { getTotalQuantity, mapFormValuesToFills } from './helpers';
import { FormValues } from './types';

type ContextProps = {
  isEditingFills: boolean;
  startEditingFills(): void;
  form: UseFormReturn<FormValues>;
  fillsQuery: ReturnType<typeof useOrderFills>;
  totalFilledQuantity: number;
  orderQuantity: number;
};

const OrderFillsContext = createContext<Nullable<ContextProps>>(null);

type Props = {
  children(value: ContextProps): ReactNode;
  order: Order;
};

export const OrderFillsProvider = (props: Props) => {
  const { children, order } = props;
  const [isEditingFills, setIsEditingFills] = useState(false);
  const fillsQuery = useOrderFills(order);
  const form = useForm<FormValues>({
    defaultValues: { fills: [] },
    mode: 'onChange',
  });
  const { mutateAsync: createFills } = useCreateFills();
  const { user } = useAuthContext();

  const {
    formState: { isSubmitSuccessful },
    handleSubmit,
    reset,
    watch,
  } = form;

  const formFills = watch('fills');

  useEffect(() => {
    if (isSubmitSuccessful) {
      reset({ fills: [] });
    }
  }, [reset, isSubmitSuccessful]);

  const startEditingFills = () => {
    setIsEditingFills(true);
  };

  const value = {
    isEditingFills,
    startEditingFills,
    form,
    fillsQuery,
    totalFilledQuantity: getTotalQuantity(fillsQuery.data, formFills),
    orderQuantity: order.baseQuantity ?? 0,
  };

  const onSubmit = async (values: FormValues) => {
    const fills = mapFormValuesToFills(values, user);
    const { id, idOrderType } = order;

    await createFills({ fills, id, idOrderType });

    setIsEditingFills(false);
  };

  return (
    <OrderFillsContext.Provider value={value}>
      <Styles.Form onSubmit={handleSubmit(onSubmit)}>
        {children(value)}
      </Styles.Form>
    </OrderFillsContext.Provider>
  );
};

export const useOrderFillsContext = () => {
  const value = useContext(OrderFillsContext);

  if (!value) {
    throw new Error(
      'useOrderFillsContext must be used within OrderFillsProvider'
    );
  }

  return value;
};
