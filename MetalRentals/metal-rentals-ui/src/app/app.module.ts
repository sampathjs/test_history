import {BrowserModule} from '@angular/platform-browser';
import {Injectable, NgModule} from '@angular/core';

import {AppComponent} from './app.component';
import {InterestRatesUpdaterComponent} from './interest-rates-updater/interest-rates-updater.component';
import {AveragePricesViewerComponent} from './average-prices-viewer/average-prices-viewer.component';
import {InterestsCalculatorComponent} from './interests-calculator/interests-calculator.component';
import {DocumentsGeneratorComponent} from './documents-generator/documents-generator.component';
import {HTTP_INTERCEPTORS, HttpClientModule, HttpHandler, HttpInterceptor, HttpRequest} from "@angular/common/http";
import {BrowserAnimationsModule} from '@angular/platform-browser/animations';
import {Backend} from "./services/backend";
import {MatInputModule} from "@angular/material/input";
import {MatButtonModule} from "@angular/material/button";
import {MatRadioModule} from "@angular/material/radio";
import {FormsModule, ReactiveFormsModule} from "@angular/forms";
import {FlexModule} from "@angular/flex-layout";
import {MatExpansionModule} from "@angular/material/expansion";
import {MatTableModule} from "@angular/material/table";
import {MatSelectModule} from "@angular/material/select";
import {MatCheckboxModule} from "@angular/material/checkbox";
import {BusyIndicatorComponent} from './busy-indicator/busy-indicator.component';
import {MatProgressSpinnerModule} from "@angular/material/progress-spinner";
import {MetalRentalsService} from "./services/metal-rentals.service";
import {UserLoginComponent} from './user-login/user-login.component';
import {MatIconModule} from "@angular/material/icon";
import {ErrorIndicatorComponent} from './error-indicator/error-indicator.component';

@Injectable()
export class XhrInterceptor implements HttpInterceptor {

    intercept(req: HttpRequest<any>, next: HttpHandler) {
        const xhr = req.clone({
            headers: req.headers.set('X-Requested-With', 'XMLHttpRequest')
        });
        return next.handle(xhr);
    }
}

@NgModule({
    declarations: [
        AppComponent,
        InterestRatesUpdaterComponent,
        AveragePricesViewerComponent,
        InterestsCalculatorComponent,
        DocumentsGeneratorComponent,
        BusyIndicatorComponent,
        UserLoginComponent,
        ErrorIndicatorComponent
    ],
    imports     : [
        BrowserModule,
        HttpClientModule,
        BrowserAnimationsModule,
        MatInputModule,
        MatButtonModule,
        MatRadioModule,
        FormsModule,
        FlexModule,
        MatExpansionModule,
        MatTableModule,
        MatSelectModule,
        ReactiveFormsModule,
        MatCheckboxModule,
        MatProgressSpinnerModule,
        MatIconModule
    ],
    providers   : [
        {provide: Backend, useClass: MetalRentalsService},
        {provide: HTTP_INTERCEPTORS, useClass: XhrInterceptor, multi: true}],
    bootstrap   : [AppComponent]
})
export class AppModule {
}
