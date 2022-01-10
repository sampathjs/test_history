import { Meta, Story } from "@storybook/angular/types-6-0";
import { moduleMetadata } from "@storybook/angular";
import { MatFormFieldModule } from "@angular/material/form-field";
import { MatSelectModule } from "@angular/material/select";
import { FormsModule } from "@angular/forms";
import { MatButtonModule } from "@angular/material/button";
import { MatInputModule } from "@angular/material/input";
import { BrowserAnimationsModule } from "@angular/platform-browser/animations";
import { BrowserModule } from "@angular/platform-browser";
import { CommonInfoComponent } from "./common-info.component";
import { MatCardModule } from "@angular/material/card";

export default {
	title: "Components/Common Info",
	decorators: [
		moduleMetadata({
			imports: [
				BrowserModule,
				BrowserAnimationsModule,
				MatFormFieldModule,
				MatSelectModule,
				MatCardModule,
				FormsModule,
				MatButtonModule,
				MatInputModule
			]
		})
	],
	component: CommonInfoComponent
} as Meta;

const Template: Story<CommonInfoComponent> = (args) => ({
	props: args
});

export const primary = Template.bind({});
primary.args = {
	currentUser: "Test User",
	groups: [
		{ name: "Group 1", companyCode: "Code 1" },
		{ name: "Group 2", companyCode: "Code 2" },
		{ name: "Group 3", companyCode: "Code 3" }
	],
	balanceDates: ["2021-01-01", "2021-02-01"],
	units: ["kg", "toz"]
};
