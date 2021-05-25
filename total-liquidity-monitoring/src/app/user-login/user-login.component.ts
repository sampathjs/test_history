import { Component, EventEmitter, Input, Output } from "@angular/core";
import { User } from "../services/user";

@Component({
	selector: "app-user-login",
	templateUrl: "./user-login.component.html",
	styleUrls: ["./user-login.component.scss"]
})
export class UserLoginComponent {
	@Input() userList: User[] | null = [];

	@Output() userSelectedEvent = new EventEmitter<string>();
	@Output() loginEvent = new EventEmitter<User>();

	selectedUser: User | undefined;
	resetPassword = false;
	nextPressed = false;
	hidePassword = true;
	password = "";

	next(): void {
		if (!this.selectedUser) {
			return;
		}
		if (this.resetPassword || this.selectedUser.needPassword) {
			this.userSelectedEvent.emit(this.selectedUser.name);
		}
		this.nextPressed = true;
	}

	login(): void {
		if (this.selectedUser) {
			this.loginEvent.emit({ name: this.selectedUser.name, password: this.password });
		}
	}
}
