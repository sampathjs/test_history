import { Meta, Story } from "@storybook/angular/types-6-0";
import { ErrorIndicatorComponent } from "./error-indicator.component";
import { moduleMetadata } from "@storybook/angular";
import { BrowserModule } from "@angular/platform-browser";
import { BrowserAnimationsModule } from "@angular/platform-browser/animations";
import { MatButtonModule } from "@angular/material/button";
import { MatDialog, MatDialogModule } from "@angular/material/dialog";
import { Component } from "@angular/core";

// noinspection AngularMissingOrInvalidDeclarationInModule
@Component({
	// eslint-disable-next-line prettier/prettier
	template: `<button mat-raised-button (click)="launch()">Open Error Indicator</button>
`
})
class LaunchComponent {
	private readonly error = "there is an error<br>the new line should work";

	constructor(private dialog: MatDialog) {}

	public launch(): void {
		this.dialog.open(ErrorIndicatorComponent, { data: this.error });
	}
}

export default {
	title: "Components/Error Indicator",
	decorators: [
		moduleMetadata({
			imports: [BrowserModule, BrowserAnimationsModule, MatButtonModule, MatDialogModule],
			entryComponents: [LaunchComponent, ErrorIndicatorComponent],
			declarations: [LaunchComponent, ErrorIndicatorComponent]
		})
	],
	component: LaunchComponent
} as Meta;

const Template: Story<LaunchComponent> = (args) => ({
	props: args
});

export const primary = Template.bind({});
