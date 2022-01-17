import { ComponentStory, Meta } from '@storybook/react';

import { ListFilter } from './ListFilter';

export default {
  title: 'Filter/ListFilter',
  component: ListFilter,
} as Meta<typeof ListFilter>;

const Template: ComponentStory<typeof ListFilter> = (args) => (
  <ListFilter {...args} />
);

export const Standard = Template.bind({});
Standard.args = {
  allOptionLabel: 'Select All',
  onChange: () => {
    return true;
  },
  options: [
    { name: 'Option 1', value: 0 },
    { name: 'Option 2', value: 1 },
    { name: 'Option 3', value: 2 },
    { name: 'Option 4', value: 3 },
    { name: 'Option 5', value: 4 },
  ],
  value: [],
};
