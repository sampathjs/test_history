import { Injectable, NgModule } from "@angular/core";
import { BrowserModule } from "@angular/platform-browser";

import { AppRoutingModule } from "./app-routing.module";
import { AppComponent } from "./app.component";
import { BusyIndicatorComponent } from "./busy-indicator/busy-indicator.component";
import { UserLoginComponent } from "./user-login/user-login.component";
import { BrowserAnimationsModule } from "@angular/platform-browser/animations";
import { MatProgressSpinnerModule } from "@angular/material/progress-spinner";
import { MatExpansionModule } from "@angular/material/expansion";
import { MatFormFieldModule } from "@angular/material/form-field";
import { MatSelectModule } from "@angular/material/select";
import { MatCheckboxModule } from "@angular/material/checkbox";
import { MatIconModule } from "@angular/material/icon";
import { FormsModule } from "@angular/forms";
import { MatButtonModule } from "@angular/material/button";
import { MatInputModule } from "@angular/material/input";
import { ErrorIndicatorComponent } from "./error-indicator/error-indicator.component";
import { CommonInfoComponent } from "./common-info/common-info.component";
import { BalanceFormComponent } from "./balance-form/balance-form.component";
import { MatCardModule } from "@angular/material/card";
import { MatTabsModule } from "@angular/material/tabs";
import { MatTableModule } from "@angular/material/table";
import {
	HTTP_INTERCEPTORS,
	HttpClientModule,
	HttpEvent,
	HttpHandler,
	HttpInterceptor,
	HttpRequest
} from "@angular/common/http";
import { Observable } from "rxjs";
import { MatDialogModule } from "@angular/material/dialog";

@Injectable()
export class XhrInterceptor implements HttpInterceptor {
	intercept(req: HttpRequest<unknown>, next: HttpHandler): Observable<HttpEvent<unknown>> {
		const xhr = req.clone({
			headers: req.headers.set("X-Requested-With", "XMLHttpRequest")
		});
		return next.handle(xhr);
	}
}

@NgModule({
	declarations: [
		AppComponent,
		BusyIndicatorComponent,
		UserLoginComponent,
		ErrorIndicatorComponent,
		CommonInfoComponent,
		BalanceFormComponent
	],
	imports: [
		BrowserModule,
		HttpClientModule,
		AppRoutingModule,
		BrowserAnimationsModule,
		MatProgressSpinnerModule,
		MatExpansionModule,
		MatFormFieldModule,
		MatSelectModule,
		MatCheckboxModule,
		MatIconModule,
		FormsModule,
		MatButtonModule,
		MatInputModule,
		MatCardModule,
		MatTabsModule,
		MatTableModule,
		MatDialogModule
	],
	providers: [{ provide: HTTP_INTERCEPTORS, useClass: XhrInterceptor, multi: true }],
	bootstrap: [AppComponent]
})
export class AppModule {}
