import React from "react";
import ReactDOM from "react-dom";
import {Provider} from "react-redux";
import {applyMiddleware, createStore} from "redux";
import {Route, Router, Switch} from "react-router-dom";
import {routerMiddleware} from "react-router-redux";
import thunk from "redux-thunk";
import createHistory from 'history/createBrowserHistory';
import rootReducer from "./modules";
import KeycloakTester from "./components/KeycloakTester";
import Keycloak from "keycloak-js";
import axios from "axios";

const history = createHistory();
const middleware = [
    thunk,
    routerMiddleware(history),
];
const store = createStore(
    rootReducer,
    applyMiddleware(...middleware)
);

const app = (realm) => (
    <Provider store={store}>
        <Router history={history}>
            <div className="container">
                <Switch>
                    <Route exact path="/" component={ () => <KeycloakTester realm={realm}/>}/>
                </Switch>
            </div>
        </Router>
    </Provider>
);

const appNotFound = (
    <div>
        Sub Domain not valid - 404
    </div>
);

const location = window.location.href;
console.log("Accessing - " + location);
const regex = /[http|https]:\/\/(.*).mikesmacbookpro.local/;

const subDomain = location.split(regex)[1] || null;
//const subDomain = "test";
let keycloakFile = "keycloak.json";
if (subDomain) {console.log(subDomain);
   keycloakFile = "/keycloak-" + subDomain + ".json";

    const kc = new Keycloak(keycloakFile);
    kc.init({onLoad: "login-required", promiseType: 'native'})
        .then((authenticated) => {
            if (authenticated) {
                store.getState().keycloak = kc;
                ReactDOM.render(app(subDomain), document.getElementById("app"));
            }
        }).catch((e) => {
            console.error(e);
            kc.logout()
        }
    );

    axios.interceptors.request.use((config) => (
        kc.updateToken(5)
            .then((refreshed) => {
                if (refreshed){
                    // This is just to force the component to refresh so we can see the tokens on screen, no need for it in production
                    store.dispatch({
                        type: "REFRESH",
                        payload: (store.getState().refresh + 1)
                    });
                }
                config.headers.Authorization = 'Bearer ' + kc.token;
                return Promise.resolve(config)
            })
            .catch((e) => {
                console.log(e);
                kc.logout();
            })
    ));
} else {
    console.log("No valid sub-domain");
    ReactDOM.render(appNotFound, document.getElementById("app"));
}
