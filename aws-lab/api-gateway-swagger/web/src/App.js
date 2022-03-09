import { useState, useEffect, useContext } from 'react';
import { Account } from './Components/Auth/Accounts';
import { BrowserRouter as Router, Route, Switch } from "react-router-dom";
import Banner from './Components/Banner';
import { Container } from 'react-bootstrap';
import ErrorDisplay from './Components/ErrorDisplay';
import Notes from './Routes/Notes'
import Login from './Routes/Login'
import Signup from './Routes/Signup'
import ErrorBoundary from './Components/ErrorBoundary'

function App() {
    const errorDelay = 5;
    const [error, setError] = useState(null)

    useEffect(() => {
        let timer1 = setTimeout(() => setError(null), errorDelay * 1000);

        return () => {
            try {
                clearTimeout(timer1);
            } catch { }
        }
    }, [error])

    return (
        <Account setError={setError} >
            <Router>
                <Banner />
                {(error) ? <ErrorDisplay error={error} /> : null}
                <Container className="main_app">
                    <Switch>
                        <Route path="/Login"><Login /></Route>
                        <Route path="/Signup"><Signup /></Route>
                        <Route path="/">
                            <ErrorBoundary>
                                <Notes setError={setError} />
                            </ErrorBoundary>
                        </Route>
                    </Switch>
                </Container>
            </Router>
        </Account>
    );

}

export default App;
