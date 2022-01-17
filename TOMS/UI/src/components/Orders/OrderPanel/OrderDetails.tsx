import { useEffect, useState } from 'react';

import { Button } from 'components/Button';
import {
  OrderPanelViewType,
  useAuthContext,
  useOrderPanelContext,
} from 'contexts';
import { useCreateComment } from 'hooks/useCreateComment';
import { useUpdateOrder } from 'hooks/useUpdateOrder';
import { Order, OrderStatusIds, OrderType, OrderTypeNameIds } from 'types';
import { formatDate, parseApiDate } from 'utils/date';
import { formatPrice, formatQuantity, formatValue } from 'utils/format';
import { formatOrderType } from 'utils/order';

import { Header } from './Header';
import {
  buildNewCommentWithAction,
  getActionLabelForStatus,
  getCancelledStatusForOrderStatus,
  getConfirmedStatusForOrderType,
  getExpiredStatusForOrderStatus,
  getLatestCommentForStatus,
  getLatestNonActionComment,
  getPulledStatusForOrderType,
  getRejectedStatusForOrderType,
  isConfirmedStatus,
  isPartiallyFilledStatus,
  isPendingStatus,
} from './helpers';
import { OrderFills } from './OrderFills';
import { OrderFillsProvider } from './OrderFillsContext';
import { OrderHistory } from './OrderHistory';
import * as Styles from './OrderPanel.styles';
import { OrderStatusComment } from './OrderStatusComment';
import { SubHeader } from './SubHeader';
import { useOrderWithComments } from './useOrderWithComments';

type Props = {
  orderId: Order['id'];
};

export const OrderDetails = (props: Props) => {
  const [isHistoryVisible, setIsHistoryVisible] = useState<boolean>(false);
  const [latestVersion, setLatestVersion] = useState<
    Order['version'] | undefined
  >(undefined);
  const [activeVersion, setActiveVersion] = useState<
    Order['version'] | undefined
  >(undefined);
  const { orderId } = props;
  const { comments, isError, isLoading, order } = useOrderWithComments(
    orderId,
    activeVersion
  );
  const { mutateAsync: updateOrder } = useUpdateOrder();
  const { mutateAsync: createComment } = useCreateComment();
  const { user } = useAuthContext();
  const { setView } = useOrderPanelContext();
  const [statusWithComment, setStatusWithComment] = useState<
    OrderStatusIds | undefined
  >();

  useEffect(() => {
    if (order && (!latestVersion || order?.version > latestVersion)) {
      setLatestVersion(order.version);
    }
  }, [order, latestVersion]);

  if (isLoading || isError || !order || !comments) {
    // TODO: replace with proper loading and error states
    return null;
  }

  const latestComment = getLatestNonActionComment(comments);
  const latestCommentForCurrentStatus = getLatestCommentForStatus(
    comments,
    order.idOrderStatus
  );

  const isLimitOrder = order.displayStringOrderType === OrderType.limit;
  const isReferenceOrder = order.displayStringOrderType === OrderType.reference;

  const updateOrderStatus = (status: OrderStatusIds) => {
    return updateOrder({ ...order, idOrderStatus: status });
  };

  const updateOrderStatusWithComment = async (
    status: OrderStatusIds,
    comment: string
  ) => {
    setStatusWithComment(undefined);

    if (!comment) {
      return updateOrder({ ...order, idOrderStatus: status });
    }

    const newCommentId = await createComment({
      comment: buildNewCommentWithAction(comment, user.id, status),
      id: order.id,
      idOrderType: order.idOrderType as OrderTypeNameIds,
    });

    await updateOrder({
      ...order,
      orderCommentIds: [...order.orderCommentIds, newCommentId],
      idOrderStatus: status,
    });
  };

  return (
    <OrderFillsProvider order={order}>
      {({
        form: {
          formState: { isSubmitting, isValid },
          reset,
        },
        isEditingFills,
        startEditingFills,
      }) => (
        <>
          <Header side={order.displayStringBuySell}>
            <Styles.HeaderTitle>
              {order.displayStringOrderType}
            </Styles.HeaderTitle>
            <Styles.Actions>
              <Button
                onClick={() => {
                  setIsHistoryVisible((isVisible) => {
                    if (!isVisible) {
                      document.getElementById('content')?.scroll({ top: 0 });
                    }

                    return !isVisible;
                  });
                }}
                variant={isHistoryVisible ? 'contained' : 'outlined'}
                color="secondary"
                size="small"
              >
                History
              </Button>
            </Styles.Actions>
            <Styles.Actions>
              <Button
                variant="outlined"
                color="secondary"
                size="small"
                onClick={() =>
                  setView({
                    type: OrderPanelViewType.NewOrder,
                    copyFromId: orderId,
                  })
                }
              >
                Copy
              </Button>
              <Button
                variant="outlined"
                color="secondary"
                size="small"
                onClick={() =>
                  setView({
                    type: OrderPanelViewType.EditOrder,
                    id: orderId,
                  })
                }
              >
                Edit
              </Button>
            </Styles.Actions>
          </Header>
          <SubHeader order={order} />
          <Styles.Content id="content">
            <OrderHistory
              visible={isHistoryVisible}
              orderId={order.id}
              activeVersion={activeVersion}
              latestVersion={latestVersion}
              onChangeVersion={setActiveVersion}
            />

            <Styles.Details>
              <Styles.Summary>
                <Styles.SectionRow>
                  <Styles.ThirdColumn>
                    <Styles.SectionRow>
                      <Styles.KeyValue>
                        <Styles.Key>Trade side</Styles.Key>
                        <Styles.TradeSide side={order.displayStringBuySell}>
                          {order.displayStringBuySell}
                        </Styles.TradeSide>
                      </Styles.KeyValue>
                      <Styles.KeyValue>
                        <Styles.Key>Order type</Styles.Key>
                        <Styles.ValueTag>
                          {formatOrderType(order.displayStringOrderType)}
                        </Styles.ValueTag>
                      </Styles.KeyValue>
                    </Styles.SectionRow>
                  </Styles.ThirdColumn>

                  <Styles.KeyValue>
                    <Styles.SecondaryKey>Order reference</Styles.SecondaryKey>
                    <Styles.Value>{formatValue(order.reference)}</Styles.Value>
                  </Styles.KeyValue>
                </Styles.SectionRow>

                <Styles.SectionRow>
                  <Styles.ThirdColumn>
                    <Styles.KeyValue>
                      <Styles.Key>JM PMM unit</Styles.Key>
                      <Styles.Value>
                        {formatValue(order.displayStringInternalBu)}
                      </Styles.Value>
                    </Styles.KeyValue>
                  </Styles.ThirdColumn>

                  <Styles.KeyValue>
                    <Styles.Key>Counterparty</Styles.Key>
                    <Styles.Value>
                      {formatValue(order.displayStringExternalBu)}
                    </Styles.Value>
                  </Styles.KeyValue>
                </Styles.SectionRow>
              </Styles.Summary>

              <Styles.PrimarySection>
                {/* TODO: Add info tooltip */}
                <Styles.SectionTitle>Metal Information</Styles.SectionTitle>
                <Styles.SectionRow>
                  <Styles.ThirdColumn>
                    <Styles.KeyValue>
                      <Styles.Key>Product ticker</Styles.Key>
                      <Styles.Value>
                        {formatValue(order.displayStringTicker)}
                      </Styles.Value>
                    </Styles.KeyValue>
                  </Styles.ThirdColumn>

                  <Styles.ThirdColumn>
                    <Styles.KeyValue>
                      <Styles.Key>Unit</Styles.Key>
                      <Styles.Value>
                        {formatValue(order.displayStringBaseQuantityUnit)}
                      </Styles.Value>
                    </Styles.KeyValue>
                  </Styles.ThirdColumn>

                  <Styles.ThirdColumn>
                    <Styles.KeyValue>
                      <Styles.Key>Metal QTY</Styles.Key>
                      <Styles.Value>
                        {formatQuantity(order.baseQuantity)}
                      </Styles.Value>
                    </Styles.KeyValue>
                  </Styles.ThirdColumn>
                </Styles.SectionRow>
              </Styles.PrimarySection>

              {isLimitOrder && (
                <>
                  <Styles.PrimarySection>
                    {/* TODO: Add info tooltip */}
                    <Styles.SectionTitle>Contract Terms</Styles.SectionTitle>
                    <Styles.SectionRow>
                      <Styles.ThirdColumn>
                        <Styles.KeyValue>
                          <Styles.Value>
                            {formatValue(order.displayStringPriceType)}
                          </Styles.Value>
                        </Styles.KeyValue>
                      </Styles.ThirdColumn>

                      <Styles.ThirdColumn>
                        <Styles.KeyValue>
                          <Styles.Value>
                            {formatValue(order.displayStringContractType)}
                          </Styles.Value>
                        </Styles.KeyValue>
                      </Styles.ThirdColumn>
                    </Styles.SectionRow>
                  </Styles.PrimarySection>

                  <Styles.SecondarySection>
                    <Styles.SectionRow>
                      <Styles.ThirdColumn>
                        <Styles.KeyValue>
                          <Styles.Key>Limit price</Styles.Key>
                          <Styles.Value>
                            {Number(formatPrice(order.limitPrice))}
                          </Styles.Value>
                        </Styles.KeyValue>
                      </Styles.ThirdColumn>

                      <Styles.ThirdColumn>
                        <Styles.KeyValue>
                          <Styles.Key>Fixed start date</Styles.Key>
                          <Styles.Value>
                            {formatDate(parseApiDate(order.startDateConcrete))}
                          </Styles.Value>
                        </Styles.KeyValue>
                      </Styles.ThirdColumn>

                      <Styles.ThirdColumn>
                        <Styles.KeyValue>
                          <Styles.Key>Settle date</Styles.Key>
                          <Styles.Value>
                            {formatDate(parseApiDate(order.settleDate))}
                          </Styles.Value>
                        </Styles.KeyValue>
                      </Styles.ThirdColumn>
                    </Styles.SectionRow>
                  </Styles.SecondarySection>

                  {![OrderStatusIds.LIMIT_PENDING].includes(
                    order.idOrderStatus
                  ) && <OrderFills />}

                  <Styles.SecondarySection>
                    <Styles.SectionRow>
                      <Styles.ThirdColumn>
                        <Styles.KeyValue>
                          <Styles.Key>Validation type</Styles.Key>
                          <Styles.Value>
                            {formatValue(order.displayStringValidationType)}
                          </Styles.Value>
                        </Styles.KeyValue>
                      </Styles.ThirdColumn>

                      <Styles.ThirdColumn>
                        <Styles.KeyValue>
                          <Styles.Key>Part fillable</Styles.Key>
                          <Styles.Value>
                            {formatValue(order.displayStringPartFillable)}
                          </Styles.Value>
                        </Styles.KeyValue>
                      </Styles.ThirdColumn>
                    </Styles.SectionRow>
                  </Styles.SecondarySection>
                </>
              )}

              <Styles.SecondarySection>
                <Styles.SectionRow>
                  <Styles.ThirdColumn>
                    <Styles.KeyValue>
                      <Styles.Key>Location</Styles.Key>
                      <Styles.Value>
                        {formatValue(order.displayStringMetalLocation)}
                      </Styles.Value>
                    </Styles.KeyValue>
                  </Styles.ThirdColumn>

                  {isReferenceOrder && <Styles.ThirdColumn />}

                  <Styles.ThirdColumn>
                    <Styles.KeyValue>
                      <Styles.Key>Metal form</Styles.Key>
                      <Styles.Value>
                        {formatValue(order.displayStringMetalForm)}
                      </Styles.Value>
                    </Styles.KeyValue>
                  </Styles.ThirdColumn>
                </Styles.SectionRow>
              </Styles.SecondarySection>

              {isReferenceOrder && (
                <Styles.SecondarySection>
                  <Styles.SecondarySectionTitle>
                    Spread
                  </Styles.SecondarySectionTitle>
                  <Styles.SectionRow>
                    <Styles.ThirdColumn>
                      <Styles.KeyValue>
                        <Styles.Key>Metal Price</Styles.Key>
                        <Styles.Value>
                          {formatPrice(order.metalPriceSpread)}
                        </Styles.Value>
                      </Styles.KeyValue>
                    </Styles.ThirdColumn>

                    <Styles.ThirdColumn>
                      <Styles.KeyValue>
                        <Styles.Key>FX Rate</Styles.Key>
                        <Styles.Value>
                          {formatPrice(order.fxRateSpread)}
                        </Styles.Value>
                      </Styles.KeyValue>
                    </Styles.ThirdColumn>

                    <Styles.ThirdColumn>
                      <Styles.KeyValue>
                        <Styles.Key>Cont/backward %</Styles.Key>
                        <Styles.Value>
                          {formatPrice(order.contangoBackwardation)}
                        </Styles.Value>
                      </Styles.KeyValue>
                    </Styles.ThirdColumn>
                  </Styles.SectionRow>
                </Styles.SecondarySection>
              )}

              <Styles.SecondarySection>
                <Styles.KeyValue>
                  <Styles.SecondaryKey>Comments</Styles.SecondaryKey>
                  {latestComment && (
                    <Styles.MultiLineValue>
                      {latestComment.commentText}
                    </Styles.MultiLineValue>
                  )}

                  {!latestComment && <Styles.Value>-</Styles.Value>}
                </Styles.KeyValue>
              </Styles.SecondarySection>

              {latestCommentForCurrentStatus && (
                <Styles.SecondarySection>
                  <Styles.KeyValue>
                    <Styles.SecondaryKey>
                      {getActionLabelForStatus(order.idOrderStatus)} Reason
                    </Styles.SecondaryKey>
                    <Styles.MultiLineValue>
                      {latestCommentForCurrentStatus.commentText}
                    </Styles.MultiLineValue>
                  </Styles.KeyValue>
                </Styles.SecondarySection>
              )}
            </Styles.Details>
          </Styles.Content>

          {statusWithComment && (
            <OrderStatusComment
              status={statusWithComment}
              onContinue={(comment) =>
                updateOrderStatusWithComment(statusWithComment, comment)
              }
            />
          )}

          {!statusWithComment && (
            <>
              {isPendingStatus(order.idOrderStatus) && (
                <Styles.Footer>
                  <Styles.Actions>
                    <Button
                      color="error"
                      onClick={() =>
                        setStatusWithComment(
                          getRejectedStatusForOrderType(order.idOrderType)
                        )
                      }
                    >
                      Reject Order
                    </Button>
                    <Button
                      variant="outlined"
                      color="error"
                      onClick={() =>
                        setStatusWithComment(
                          getPulledStatusForOrderType(order.idOrderType)
                        )
                      }
                    >
                      Pull Order
                    </Button>
                  </Styles.Actions>
                  <Styles.Actions>
                    <Button
                      onClick={() =>
                        updateOrderStatus(
                          getConfirmedStatusForOrderType(order.idOrderType)
                        )
                      }
                    >
                      Confirm Order
                    </Button>
                  </Styles.Actions>
                </Styles.Footer>
              )}

              {isLimitOrder &&
                (isConfirmedStatus(order.idOrderStatus) ||
                  isPartiallyFilledStatus(order.idOrderStatus)) &&
                !isEditingFills && (
                  <Styles.Footer>
                    <Styles.Actions>
                      <Button
                        color="error"
                        onClick={() =>
                          setStatusWithComment(
                            getCancelledStatusForOrderStatus(
                              order.idOrderStatus
                            )
                          )
                        }
                      >
                        Cancel Order
                      </Button>
                      <Button
                        variant="outlined"
                        color="error"
                        onClick={() =>
                          updateOrderStatus(
                            getExpiredStatusForOrderStatus(order.idOrderStatus)
                          )
                        }
                      >
                        Expire Order
                      </Button>
                    </Styles.Actions>
                    <Styles.Actions>
                      <Button onClick={startEditingFills}>Fill Order</Button>
                    </Styles.Actions>
                  </Styles.Footer>
                )}

              {isEditingFills && (
                <Styles.Footer>
                  <Styles.Actions>
                    <Button
                      variant="outlined"
                      color="secondary"
                      type="button"
                      onClick={() => reset()}
                    >
                      Reset
                    </Button>
                  </Styles.Actions>
                  <Styles.Actions>
                    <Button type="submit" disabled={isSubmitting || !isValid}>
                      Submit Order
                    </Button>
                  </Styles.Actions>
                </Styles.Footer>
              )}
            </>
          )}
        </>
      )}
    </OrderFillsProvider>
  );
};
