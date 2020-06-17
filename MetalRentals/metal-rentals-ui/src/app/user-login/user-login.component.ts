import {Component, EventEmitter, OnInit, Output} from '@angular/core';
import {Backend} from "../services/backend";
import {User} from "./user";
import {finalize} from "rxjs/operators";

@Component({
    selector   : 'app-user-login',
    templateUrl: './user-login.component.html',
    styleUrls  : ['./user-login.component.css']
})
export class UserLoginComponent implements OnInit {

    @Output() loginEvent = new EventEmitter<Boolean>();

    isBusy: boolean = false;
    error: string;
    userList: User[];
    selectedUser: User;
    resetPassword: boolean = false;
    userSelected: boolean = false;
    hidePassword: boolean = true;
    password: string = "";

    constructor(private backend: Backend) {
    }

    ngOnInit() {
        this.isBusy = true;
        this.backend.getUserList().pipe(finalize(() => this.isBusy = false)).subscribe(data => {
            this.userList = data;
        }, error => {
            this.error = error;
        });
    }

    next() {
        if (this.resetPassword || this.selectedUser.needPassword) {
            this.isBusy = true;
            this.backend.resetPassword(this.selectedUser.username)
                .pipe(finalize(() => this.isBusy = false))
                .subscribe(() => {
                    this.userSelected = true;
                }, error => {
                    this.error = error;
                });
        } else {
            this.userSelected = true;
        }
    }

    login() {
        this.isBusy = true;
        this.backend.login(this.selectedUser.username, this.password)
            .pipe(finalize(() => this.isBusy = false)).subscribe(() => {
            this.loginEvent.emit(true);
        }, error => {
            this.error = error;
        });
    }
}
