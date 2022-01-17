import { OrderPanelViewType, useOrderPanelContext } from 'contexts/OrderPanel';

import { EditOrder } from './EditOrder';
import { NewOrder } from './NewOrder';
import { OrderDetails } from './OrderDetails';
import * as Styles from './OrderPanel.styles';

export const OrderPanel = () => {
  const { view } = useOrderPanelContext();

  return (
    <Styles.Wrapper $isOpen={!!view}>
      {view && (
        <>
          {view.type === OrderPanelViewType.NewOrder && (
            <NewOrder copyFromId={view.copyFromId} />
          )}
          {view.type === OrderPanelViewType.EditOrder && (
            <EditOrder orderId={view.id} />
          )}
          {view.type === OrderPanelViewType.OrderDetails && (
            // the key is used here to force create a new instance for different order id
            // so that there is no need to reset internal instance state manually
            <OrderDetails orderId={view.id} key={view.id} />
          )}
        </>
      )}
    </Styles.Wrapper>
  );
};
