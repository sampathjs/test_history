import { ComponentStory, Meta } from '@storybook/react';

import { Select } from './';

export default {
  title: 'Form/Components/Select',
  component: Select,
} as Meta<typeof Select>;

const Template: ComponentStory<typeof Select> = (args) => <Select {...args} />;

export const Default = Template.bind({});
Default.args = {
  options: [
    {
      label: 'Foo A1',
      value: 'bara1',
    },
    {
      label: 'Foo A2',
      value: 'bara2',
    },
  ],
  onChange: () => null,
};

export const WithSelectedOption = Template.bind({});
WithSelectedOption.args = {
  options: [
    {
      label: 'Foo A1',
      value: 'bara1',
    },
    {
      label: 'Foo A2',
      value: 'bara2',
    },
  ],
  value: {
    label: 'Foo A1',
    value: 'bara1',
  },
  onChange: () => null,
};

export const WithGroups = Template.bind({});
WithGroups.args = {
  options: [
    {
      label: 'Group A',

      options: [
        {
          label: 'Foo A1',
          value: 'bara1',
        },
        {
          label: 'Foo A2',
          value: 'bara2',
        },
      ],
    },
    {
      label: 'Group B',

      options: [
        {
          label: 'Foo B1',
          value: 'barb1',
        },
        {
          label: 'Foo B2',
          value: 'barb2',
        },
      ],
    },
  ],
  onChange: () => null,
};
