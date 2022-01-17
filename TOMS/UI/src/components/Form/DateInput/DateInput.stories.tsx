import { ComponentStory, Meta } from '@storybook/react';
import { useForm } from 'react-hook-form';

import { getDateToday } from 'utils/date';

import { DateInput } from '.';

export default {
  title: 'Form/Inputs/DateInput',
  component: DateInput,
} as Meta<typeof DateInput>;

const Template: ComponentStory<typeof DateInput> = (args) => {
  const { control } = useForm();

  return <DateInput {...args} name="myDate" control={control} />;
};

export const Default = Template.bind({});
Default.args = {};

export const WithDefaultDate = Template.bind({});
WithDefaultDate.args = {
  defaultValue: getDateToday().toJSDate(),
};

export const WithMinDate = Template.bind({});
WithMinDate.args = {
  minDate: getDateToday().toJSDate(),
};
