import { ComponentStory, Meta } from '@storybook/react';
import { useForm } from 'react-hook-form';

import { TextInput } from '.';

export default {
  title: 'Form/Inputs/TextInput',
  component: TextInput,
} as Meta<typeof TextInput>;

const Template: ComponentStory<typeof TextInput> = (args) => {
  const { control } = useForm();

  return <TextInput {...args} name="orderRef" control={control} />;
};

export const Default = Template.bind({});
Default.args = {};

export const WithDefaultValue = Template.bind({});
WithDefaultValue.args = {
  defaultValue: 'Foobar',
};
