import { ComponentStory, Meta } from '@storybook/react';
import { useForm } from 'react-hook-form';

import { SelectInput } from '.';

export default {
  title: 'Form/Inputs/SelectInput',
  component: SelectInput,
} as Meta<typeof SelectInput>;

const Template: ComponentStory<typeof SelectInput> = (args) => {
  const { control } = useForm();

  return (
    <SelectInput
      {...args}
      name="metalUnit"
      control={control}
      options={[
        { label: 'TOz', value: 28 },
        { label: 'MT', value: 29 },
        { label: 'gms', value: 30 },
        { label: 'kgs', value: 31 },
        { label: 'lbs', value: 32 },
        { label: 'mgs', value: 33 },
      ]}
    />
  );
};

export const Default = Template.bind({});
Default.args = {};

export const WithDefaultValue = Template.bind({});
WithDefaultValue.args = {
  defaultValue: { label: 'TOz', value: 28 },
};
