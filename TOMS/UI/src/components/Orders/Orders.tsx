import {
  FilterProvider,
  OrderPanelProvider,
  TimeZoneProvider,
  useLayoutContext,
} from 'contexts';

import { Filters } from './Filters';
import { Grid } from './Grid';
import { Header } from './Header';
import { OrderPanel } from './OrderPanel';
import * as Styles from './Orders.styles';

export const Orders = () => {
  const { filter } = useLayoutContext();

  return (
    <FilterProvider>
      <TimeZoneProvider>
        <OrderPanelProvider>
          <Styles.Layout $isFilterPanelExpanded={filter.isPanelExpanded}>
            <Filters />
            <Styles.Main>
              <Header />
              <Styles.Content>
                <Grid />
                <OrderPanel />
              </Styles.Content>
            </Styles.Main>
          </Styles.Layout>
        </OrderPanelProvider>
      </TimeZoneProvider>
    </FilterProvider>
  );
};
