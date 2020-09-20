import React from "react";
import axios from "axios";

export default class AdminAction extends React.Component {
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

  adminFunc = () => {
    axios.get('http://localhost:3000/admin')
      .then((res) => {
        this.setResponse(res.statusText)
      }).catch((err) => {
        this.setResponse(err.toString())
    })
  };

  render() {
    return (

      <div>
        <button onClick={this.adminFunc}>Role Based Protected Resource</button>
        <p><b>GET /protectedWithRole</b></p>
        {this.renderMessage()}
      </div>
    )
  }
}
