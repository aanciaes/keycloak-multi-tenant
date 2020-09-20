import React from "react";
import axios from "axios";


export default class PublicAction extends React.Component {
    state = {
        response: null
    };

    renderMessage = () => {
        if (this.state.response) {
            return (<div>{this.state.response}</div>)
        } else {
            return null;
        }
    };

    setResponse = (r) => {
        this.setState({response: r});
        setTimeout(() => this.setState({response: null}), 5000);
    };

    publicFunc = () => {
        axios.get('http://localhost:3000/public')
            .then((res) => {
                this.setResponse(res.statusText);
            }).catch((err) => {
            this.setResponse(err.toString());
        })
    };

    render() {
        return (

            <div>
                <button onClick={this.publicFunc}>Unprotected Operation</button>
                <p><b>GET /public</b></p>
                {this.renderMessage()}
            </div>
        )
    }
}
