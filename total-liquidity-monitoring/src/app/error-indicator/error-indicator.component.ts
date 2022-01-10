import { Component, Inject } from "@angular/core";
import { MAT_DIALOG_DATA } from "@angular/material/dialog";

@Component({
	selector: "app-error-indicator",
	templateUrl: "./error-indicator.component.html",
	styleUrls: ["./error-indicator.component.scss"]
})
export class ErrorIndicatorComponent {
	constructor(@Inject(MAT_DIALOG_DATA) public error: string) {}

	displayError(): string {
		return this.error ? `An error has occurred: <br><br>${this.error}` : "";
	}
}
