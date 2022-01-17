import { ComponentStory, Meta } from '@storybook/react';

import { Checkbox } from 'components/Checkbox';

import { FormControlLabel } from './FormControlLabel';

export default {
  title: 'FormControlLabel',
  component: FormControlLabel,
} as Meta<typeof FormControlLabel>;

const Template: ComponentStory<typeof FormControlLabel> = (args) => (
  <FormControlLabel {...args} />
);

export const Standard = Template.bind({});
Standard.args = {
  label: 'Select all',
  control: <Checkbox name="my-checkbox" value="1" checked />,
};
