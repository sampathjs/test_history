import { Meta, Story } from "@storybook/angular/types-6-0";
import { UserLoginComponent } from "./user-login.component";
import { moduleMetadata } from "@storybook/angular";
import { MatExpansionModule } from "@angular/material/expansion";
import { MatFormFieldModule } from "@angular/material/form-field";
import { MatSelectModule } from "@angular/material/select";
import { MatCheckboxModule } from "@angular/material/checkbox";
import { MatIconModule } from "@angular/material/icon";
import { FormsModule } from "@angular/forms";
import { MatButtonModule } from "@angular/material/button";
import { MatInputModule } from "@angular/material/input";
import { BrowserAnimationsModule } from "@angular/platform-browser/animations";
import { BrowserModule } from "@angular/platform-browser";
import { action } from "@storybook/addon-actions";
import { MatCardModule } from "@angular/material/card";

export default {
	title: "Components/User Login",
	decorators: [
		moduleMetadata({
			imports: [
				BrowserModule,
				BrowserAnimationsModule,
				MatExpansionModule,
				MatFormFieldModule,
				MatSelectModule,
				MatCheckboxModule,
				MatIconModule,
				FormsModule,
				MatButtonModule,
				MatInputModule,
				MatCardModule
			]
		})
	],
	component: UserLoginComponent
} as Meta;

const Template: Story<UserLoginComponent> = (args) => ({
	props: {
		...args,
		userSelectedEvent: action("userSelectedEvent"),
		loginEvent: action("loginEvent")
	}
});

export const primary = Template.bind({});
primary.args = {
	userList: [{ name: "user 1" }, { name: "user 2" }, { name: "user user" }]
};
