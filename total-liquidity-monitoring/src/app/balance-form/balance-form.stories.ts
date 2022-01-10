import { moduleMetadata } from "@storybook/angular";
import { BrowserModule } from "@angular/platform-browser";
import { BrowserAnimationsModule } from "@angular/platform-browser/animations";
import { MatFormFieldModule } from "@angular/material/form-field";
import { MatSelectModule } from "@angular/material/select";
import { FormsModule } from "@angular/forms";
import { MatButtonModule } from "@angular/material/button";
import { MatInputModule } from "@angular/material/input";
import { Meta, Story } from "@storybook/angular/types-6-0";
import { MatTabsModule } from "@angular/material/tabs";
import { MatTableModule } from "@angular/material/table";
import { BalanceFormComponent } from "./balance-form.component";
import { action } from "@storybook/addon-actions";
import { MatIconModule } from "@angular/material/icon";
import { Balance } from "./balance";

export default {
	title: "Components/Balance Form",
	decorators: [
		moduleMetadata({
			imports: [
				BrowserModule,
				BrowserAnimationsModule,
				MatFormFieldModule,
				MatSelectModule,
				FormsModule,
				MatIconModule,
				MatButtonModule,
				MatInputModule,
				MatTabsModule,
				MatTableModule
			]
		})
	],
	component: BalanceFormComponent
} as Meta;

const Template: Story<BalanceFormComponent> = (args) => ({
	props: {
		...args,
		saveEvent: action("saveEvent"),
		errorEvent: action("errorEvent")
	}
});

export const primary = Template.bind({});
primary.args = {
	currentUser: "Test User",
	group: "JM Plc",
	balanceDate: "2021-01-01",
	existingForecasts: [
		{
			user: "Test User",
			group: "JM Plc",
			metal: "XOS",
			balanceDate: "2021-01-01",
			companyCode: "000",
			comments: "",
			unit: "kg",
			deliverable: 1000,
			balances: [new Balance("A Customer"), new Balance("B Customer")]
		}
	],
	metals: ["XOS", "XRU", "XIR", "XPT", "XPD", "XRH"],
	customers: ["A Customer", "B Customer", "C Customer"],
	basisOfAssumptions: ["Contractual", "Average", "Estimate"]
};
