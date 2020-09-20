import React from "react";
import axios from "axios";

export default class ResourceAccess extends React.Component {
    state = {
        response: null,
        resource: "satellite",
        operation: "get"
    };

    onSelect = (event) => {
        this.setState({[event.target.name]: event.target.value});
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

    adminFunc = () => {
        const url = `http://api.mikesmacbookpro.local:8888/${this.state.resource}`;
        switch (this.state.operation) {
            case "get":
                axios.get(url)
                    .then((res) => {
                        this.setResponse(res.statusText)
                    }).catch((err) => {
                    this.setResponse(err.toString())
                });
                break;
            case "post":
                axios.post(url)
                    .then((res) => {
                        this.setResponse(res.statusText)
                    }).catch((err) => {
                    this.setResponse(err.toString())
                });
                break;
            case "put":
                axios.put(url)
                    .then((res) => {
                        this.setResponse(res.statusText)
                    }).catch((err) => {
                    this.setResponse(err.toString())
                });
                break;
            case "delete":
                axios.delete(url)
                    .then((res) => {
                        this.setResponse(res.statusText)
                    }).catch((err) => {
                    this.setResponse(err.toString())
                });
                break;
        }
    };

    render() {
        return (
            <div>
                <select id="operation" name="operation" value={this.state.operation} onChange={this.onSelect}>
                    <option value="get">GET</option>
                    <option value="post">POST</option>
                    <option value="put">PUT</option>
                    <option value="delete">DELETE</option>
                </select>
                <br/>
                <br/>
                <select id="resource" name="resource" value={this.state.resource} onChange={this.onSelect}>
                    <option value="satellite" selected>satellite</option>
                    <option value="constellation">constellation</option>
                </select>

                <br/>
                <br/>
                <button onClick={this.adminFunc}>{this.state.operation} on {this.state.resource}</button>
                {this.renderMessage()}
            </div>
        )
    }
}
