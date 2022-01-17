import { ComponentMeta, ComponentStory } from '@storybook/react';

import { Nav } from './Nav';

export default {
  title: 'Layout/Nav',
  component: Nav,
} as ComponentMeta<typeof Nav>;

const Template: ComponentStory<typeof Nav> = () => <Nav />;

export const Standard = Template.bind({});
Standard.args = {};
