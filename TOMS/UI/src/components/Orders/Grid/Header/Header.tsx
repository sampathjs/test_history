import { Button } from 'components/Button';
import { SettingsIcon } from 'components/Icon';
import { IconButton } from 'components/IconButton';
import {
  OrderPanelViewType,
  useOrderPanelContext,
  useTimeZoneContext,
} from 'contexts';
import { TimeZone } from 'types/date';

import * as Styles from './Header.styles';

type Props = {
  onSettingsClick(): void;
  totalNumberOfOrders: number;
};

export const Header = (props: Props) => {
  const { onSettingsClick, totalNumberOfOrders } = props;
  const { setView } = useOrderPanelContext();
  const { setTimeZone, timeZone } = useTimeZoneContext();

  return (
    <Styles.Header>
      <Styles.Title>Open Orders ({totalNumberOfOrders})</Styles.Title>
      <Styles.Actions>
        <Styles.Action>
          <Styles.TimeZoneTitle>Time Zone</Styles.TimeZoneTitle>
        </Styles.Action>
        <Styles.Action>
          <Styles.TimeZoneSelector>
            {Object.entries(TimeZone).map(([key, value]) => (
              <Styles.TimeZoneSelectButton
                key={key}
                active={timeZone === value}
                onClick={() => setTimeZone(value)}
              >
                {key}
              </Styles.TimeZoneSelectButton>
            ))}
          </Styles.TimeZoneSelector>
        </Styles.Action>
        <Styles.Action>
          <Button
            variant="outlined"
            onClick={() => setView({ type: OrderPanelViewType.NewOrder })}
          >
            Add order
          </Button>
        </Styles.Action>
        <Styles.Action>
          <IconButton onClick={onSettingsClick}>
            <SettingsIcon />
          </IconButton>
        </Styles.Action>
      </Styles.Actions>
    </Styles.Header>
  );
};
