import React, { useState } from 'react';

import { Button } from 'components/Button';
import { OrderStatusIds } from 'types';

import {
  getActionLabelForStatus,
  getPlaceholderLabelForStatus,
} from './helpers';
import * as Styles from './OrderStatusComment.styles';

type Props = {
  status: OrderStatusIds;
  onContinue(comment: string): void;
};

export const OrderStatusComment = (props: Props) => {
  const { onContinue, status } = props;
  const [comment, setComment] = useState('');

  return (
    <Styles.Wrapper>
      <Styles.Title>Please tell us why (optional)</Styles.Title>
      <Styles.Comment
        placeholder={`Reason for ${getPlaceholderLabelForStatus(
          status
        )} this order`}
        value={comment}
        onChange={(event) => setComment(event.target.value)}
      />
      <Styles.Actions>
        <Button onClick={() => onContinue(comment)}>
          Continue to {getActionLabelForStatus(status)} order
        </Button>
      </Styles.Actions>
    </Styles.Wrapper>
  );
};
