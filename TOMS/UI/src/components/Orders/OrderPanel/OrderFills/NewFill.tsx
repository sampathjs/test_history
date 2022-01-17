import { TextInput } from 'components/Form/TextInput';
import { useAuthContext } from 'contexts';
import { formatShortDateTime, getCurrentDateTime } from 'utils/date';

import { useOrderFillsContext } from '../OrderFillsContext';
import * as Styles from './OrderFills.styles';

type Props = {
  index: number;
};

export const NewFill = (props: Props) => {
  const { index } = props;
  const { form, orderQuantity, totalFilledQuantity } = useOrderFillsContext();
  const { user } = useAuthContext();

  return (
    <Styles.TableRow>
      <Styles.TableInputCell>
        <TextInput
          name={`fills.${index}.volume`}
          control={form.control}
          type="number"
          rules={{
            deps: ['fills'],
            required: true,
            min: 1,
            validate: () => totalFilledQuantity <= orderQuantity,
          }}
        />
      </Styles.TableInputCell>
      <Styles.TableInputCell>
        <TextInput
          name={`fills.${index}.price`}
          control={form.control}
          type="number"
          rules={{ required: true, min: 0 }}
        />
      </Styles.TableInputCell>
      <Styles.TableCell>
        {formatShortDateTime(getCurrentDateTime())}
      </Styles.TableCell>
      <Styles.TableCell>{user.lastName}</Styles.TableCell>
      <Styles.TableCell>-</Styles.TableCell>
      <Styles.TableCell>-</Styles.TableCell>
    </Styles.TableRow>
  );
};
