import { Order } from 'types';

export enum OrderPanelViewType {
  NewOrder = 'NewOrder',
  EditOrder = 'EditOrder',
  OrderDetails = 'OrderDetails',
}

type NewOrderView = {
  type: OrderPanelViewType.NewOrder;
  copyFromId?: Order['id'];
};

type EditOrderView = {
  type: OrderPanelViewType.EditOrder;
  id: Order['id'];
};

type OrderDetailsView = {
  type: OrderPanelViewType.OrderDetails;
  id: Order['id'];
};

export type OrderPanelView = NewOrderView | EditOrderView | OrderDetailsView;
