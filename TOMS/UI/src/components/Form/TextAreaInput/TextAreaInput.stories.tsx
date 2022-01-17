import { ComponentStory, Meta } from '@storybook/react';
import { useForm } from 'react-hook-form';

import { TextAreaInput } from '.';

export default {
  title: 'Form/Inputs/TextAreaInput',
  component: TextAreaInput,
} as Meta<typeof TextAreaInput>;

const Template: ComponentStory<typeof TextAreaInput> = (args) => {
  const { control } = useForm();

  return <TextAreaInput {...args} name="comments" control={control} />;
};

export const Default = Template.bind({});
Default.args = {};

export const WithPlaceholderValue = Template.bind({});
WithPlaceholderValue.args = {
  placeholder: 'Foo bar baz',
};
