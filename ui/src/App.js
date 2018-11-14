import React, {Component} from 'react';
import {RSocketClient} from 'rsocket-core';
import RSocketWebSocketClient from "rsocket-websocket-client";
import timeago from 'timeago.js';
import {Input} from "pivotal-ui/react/inputs/input";
import {Grid} from "pivotal-ui/react/flex-grids/grid";
import {FlexCol} from "pivotal-ui/react/flex-grids/flex-col";
import {PrimaryButton} from 'pivotal-ui/react/buttons';
import {Icon} from 'pivotal-ui/react/iconography';
import {ProgressBar} from 'pivotal-ui/react/progress-bar';
import 'pivotal-ui/css/border';
import 'pivotal-ui/css/box-shadows';
import 'pivotal-ui/css/alignment';
import 'pivotal-ui/css/links';
import 'pivotal-ui/css/typography';
import 'pivotal-ui/css/alerts';
import 'pivotal-ui/css/forms';

import './App.css';


function BattleLog(props) {
    const ago = timeago().format(new Date(Number(props.battleLog.metadata)));
    return (
        <div className={"mvxl paxl box-shadow-amb-1 alert pui-alert battle-log " + (props.battleLog.data.startsWith(props.userName + ":") ? "alert-info pui-alert-info" : "")}>
            {props.battleLog.data.startsWith("Waiting") && (<Icon style={{'fontSize': '16px'}} src="spinner-sm"/>)}
            &nbsp;
            {props.battleLog.data}
            &nbsp;
            <span className="type-sm type-neutral-4">{ago}</span>
        </div>
    );
}

class App extends Component {
    constructor(props) {
        super(props);
        this.state = {
            battleLogs: [],
            userName: "",
            userPower: 300,
            enemyName: "",
            enemyPower: 300,
            offensivePower: 100,
            maxInFlight: 5,
            inFlight: false
        };
        this.client = new RSocketClient({
            setup: {
                dataMimeType: 'text/plain',
                metadataMimeType: 'text/plain'
            },
            transport: new RSocketWebSocketClient({url: 'ws://localhost:8800'}),
        });
        this.rsocket = null;
    }

    componentDidMount() {
        window.addEventListener('beforeunload', e => {
            if (this.state.inFlight) {
                const confirmMessage = 'The battle is still in flight.';
                e.returnValue = confirmMessage;
                return confirmMessage;
            } else {
                return null;
            }
        });
    }

    connect() {
        if (!this.rsocket) {
            this.rsocket = this.client.connect();
        }
        this.setState({battleLogs: [], inFlight: true});

        const pattern = /(.+):\tremaining power is (\d+)\w*/;
        this.rsocket.subscribe({
            onComplete: socket => {
                const flowable = socket.requestStream({
                    data: this.state.userName + " " + this.state.offensivePower
                });
                let current = this.state.maxInFlight;
                let subscription;
                flowable.subscribe({
                    onComplete: () => {
                        this.setState({inFlight: false});
                    },
                    onError: error => {
                        this.setState({inFlight: false});
                        console.error(error);
                        alert(error);
                    },
                    onNext: value => {
                        const battleLogs = this.state.battleLogs;
                        const group = value.data.match(pattern);
                        if (group) {
                            console.log(group[1], group[2]);
                            if (group[1] === this.state.userName) {
                                this.setState({userPower: Number(group[2])});
                            } else {
                                this.setState({
                                    enemyName: group[1],
                                    enemyPower: Number(group[2])
                                });
                            }
                        }
                        battleLogs.unshift(value);
                        while (battleLogs.length > 200) {
                            battleLogs.pop();
                        }
                        this.setState({battleLogs: battleLogs});
                        current--;
                        if (current === 0) {
                            current = this.state.maxInFlight;
                            setTimeout(() => {
                                subscription.request(this.state.maxInFlight);
                            }, 500);
                        }
                    },
                    onSubscribe: sub => {
                        subscription = sub;
                        subscription.request(this.state.maxInFlight);
                    }
                });
            },
            onError: error => {
                console.error(error);
                alert(error);
            },
        });
    }

    changeMaxInFlight(event) {
        this.setState({maxInFlight: Number(event.target.value)});
    }

    isWaiting() {
        return this.state.inFlight && this.state.battleLogs.length === 0;
    }

    canSubmit() {
        return !(this.state.userName.length > 0 && this.state.offensivePower > 0 && this.state.offensivePower < 200) || this.state.inFlight;
    }

    render() {
        return (
            <div className="App">
                <form onSubmit={(e) => {
                    e.preventDefault();
                    this.connect();
                }}>
                    <div>
                        <Grid>
                            <div className="col">
                                <div className="form-unit">
                                    <div className="grid grid-nogutter label-row">
                                        <div className="col">
                                            <label htmlFor="userName">User Name</label>
                                        </div>
                                        <div className="col col-fixed col-middle post-label"></div>
                                    </div>
                                    <div className="field-row">
                                        <Input id="userName" name="userName" required=""
                                               value={this.state.userName}
                                               disabled={this.state.inFlight}
                                               onChange={(e) => this.setState({userName: e.target.value})}/>
                                    </div>
                                    <div className="help-row type-dark-5">must not be empty.</div>
                                </div>
                            </div>
                            <div className="col">
                                <div className="form-unit">
                                    <div className="grid grid-nogutter label-row">
                                        <div className="col">
                                            <label htmlFor="offensivePower">Offense</label>
                                        </div>
                                        <div className="col col-fixed col-middle post-label"></div>
                                    </div>
                                    <div className="field-row">
                                        <Input id="offensivePower" name="offensivePower" type="number"
                                               required="" min={1} max={199}
                                               value={this.state.offensivePower}
                                               disabled={this.state.inFlight}
                                               onChange={(e) => this.setState({offensivePower: Number(e.target.value)})}/>
                                    </div>
                                    <div className="help-row type-dark-5">Offense + Defense must be 200.</div>
                                </div>
                            </div>
                            <div className="col">
                                <div className="form-unit">
                                    <div className="grid grid-nogutter label-row">
                                        <div className="col">
                                            <label>Defense</label>
                                        </div>
                                        <div className="col col-fixed col-middle post-label"></div>
                                    </div>
                                    <div className="field-row">
                                        <Input type="number" name="defensivePower"
                                               value={200 - this.state.offensivePower}
                                               disabled={true}/>
                                    </div>
                                    <div className="help-row type-dark-5"></div>
                                </div>
                            </div>
                        </Grid>
                        <Grid>
                            <div className="col">
                                <div className="form-unit inline-form-unit">
                                    <div className="grid grid-inline">
                                        <div className="col col-fixed field-row">

                                            request(<Input type="number"
                                                           min={1} max={256}
                                                           style={{width: "1em"}}
                                                           value={this.state.maxInFlight}
                                                           onChange={(e) => this.setState({maxInFlight: Number(e.target.value)})}/>)
                                        </div>
                                    </div>
                                    <div className="grid">
                                        <div className="col help-row type-dark-5"></div>
                                    </div>
                                </div>
                            </div>
                        </Grid>
                        <Grid>
                            <FlexCol fixed>
                                <PrimaryButton
                                    type="submit"
                                    disabled={this.canSubmit()}>Start a
                                    battle</PrimaryButton>
                            </FlexCol>
                        </Grid>
                    </div>
                </form>
                <hr/>
                <div>
                    <h3>Power Gauge</h3>
                    <div style={{display: this.state.userName.length > 0 ? "block" : "none"}}>
                        <span>{this.state.userName}</span>
                        <ProgressBar value={this.state.userPower / 3} barClassName='bar-class'/>
                        <Grid>
                            <FlexCol>{this.state.userPower} / 300</FlexCol>
                        </Grid>
                    </div>
                    <div style={{display: this.state.enemyName.length > 0 ? "block" : "none"}}>
                        <span>{this.state.enemyName}</span>
                        <ProgressBar value={this.state.enemyPower / 3} barClassName='bar-class'/>
                        <Grid>
                            <FlexCol>{this.state.enemyPower} / 300</FlexCol>
                        </Grid>
                    </div>
                </div>
                <h3>Battle Log</h3>
                {this.isWaiting() && <BattleLog userName={this.state.userName}
                                                battleLog={{data: "Waiting for the battle", metadata: new Date().getTime()}}/>}
                {this.state.battleLogs
                    .map((battleLog) =>
                        <BattleLog key={battleLog.meta + "" + Math.ceil(Math.random() * 10000000)}
                                   userName={this.state.userName}
                                   battleLog={battleLog}/>)}
            </div>
        );
    }
}

export default App;
