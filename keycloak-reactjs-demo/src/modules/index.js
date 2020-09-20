import {combineReducers} from "redux";
import {routerReducer} from "react-router-redux";

export default combineReducers({
  refresh: (refresh = 0, action) => {
    switch (action.type) {
      case "REFRESH":
        return  action.payload;

      default:
        return refresh;
    }
  },
  keycloak: (keycloak = {}) => keycloak,
  routing: routerReducer,
});
