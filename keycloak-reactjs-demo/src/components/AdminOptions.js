import * as React from "react";
import axios from "axios";

export class AdminOptions extends React.Component {
    state = {
        username: null,
        email: null,
        password: null,
        temporaryCredentials: false,
        role: "basic",
        message: null
    };

    setResponse = (r) => {
        this.setState({message: r});
        setTimeout(() => this.setState({message: null}), 5000);
    };

    createNewUser = () => {
        axios.post('http://localhost:3000/user', {
            headers: {
                "Content-Type": "application/json"
            },
            data: {
                username: this.state.username,
                email: this.state.email,
                password: this.state.password,
                temporaryCredentials: this.state.temporaryCredentials,
                role: this.state.role
            }
        }).then((res) => {
                this.setResponse(res.statusText)
            }).catch((err) => {
                this.setResponse(err.toString())
            }
        )
    };

    render() {
        return (
            <div>
                <div>
                    <h3>New User</h3><br/>
                    username: <input name={"username"} onChange={(e) => {this.setState({username: e.target.value})}}/><br/>
                    email: <input name={"email"} onChange={(e) => {this.setState({email: e.target.value})}}/><br/>
                    password: <input name={"password"} onChange={(e) => {this.setState({password: e.target.value})}}/><br/>

                    Temporary Credentials:
                    <input type="checkbox" onChange={(e) => {this.setState({temporaryCredentials: e.target.checked})}}/><br/>

                    role:
                    <select value={this.state.role} onChange={(e) => {this.setState({role: e.target.value})}}>
                        <option>basic</option>
                        <option>admin</option>
                    </select>

                    <br/>
                    <button style={{marginTop: "10px", marginBottom: "50px"}} onClick={() => this.createNewUser()}>Create new User</button>
                    <br/>
                    <p style={{paddingBottom: "50px"}}>{this.state.message}</p>
                </div>
            </div>
        )
    }
}
