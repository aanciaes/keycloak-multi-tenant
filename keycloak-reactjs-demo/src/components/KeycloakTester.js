import React from "react";
import {connect} from "react-redux";
import ResourceAccess from "./ResourceAccess";

class KeycloakTester extends React.Component {
    render() {
        const {kc} = this.props;

        return (
            <div className="bookBox row">
                <h1>
                    Welcome {kc.tokenParsed.preferred_username} to realm {this.props.realm} &nbsp;
                    <button className="btn btn-success" onClick={kc.logout}>Logout</button>
                </h1>
                <br/>
                {/*<h2>Backend Roles: {kc.tokenParsed.resource_access["backend"].roles.join(", ")}</h2>*/}
                <p>refreshToken: {kc.refreshToken}</p>
                <p>accessToken: {kc.token}</p>

                <br/>
                <ResourceAccess/>
                {/*<PublicAction token={kc.token}/>
                <br/>
                <ProtectedAction token={kc.token}/>
                <br/>
                <AdminAction token={kc.token}/>*/}

                {/* <div>
                    <h2>Administrator Options</h2>
                    <AdminOptions/>
                </div>*/}
            </div>
        );
    }
}

const mapStateToProps = state => ({
    kc: state.keycloak,
    refresh: state.refresh
});

export default connect(mapStateToProps, null)(KeycloakTester)
