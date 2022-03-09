import { useState, useEffect, useContext } from 'react';
import { useHistory } from 'react-router-dom';
import PollyWindow from '../Components/Notes/PollyWindow'
import { AccountContext } from '../Components/Auth/Accounts';
import NoteDisplay from '../Components/Notes/NoteDisplay';
import NoteEntry from '../Components/Notes/NoteEntry';
import LoadingSpinner from '../Components/LoadingSpinner';
import Config from '../Config';

function Notes(props) {
    const [notes, setNotes] = useState([])
    const [selNote, setSelNote] = useState({ "NoteId": "" })
    const [notesLoaded, setNotesLoaded] = useState(false)
    const [updating, setUpdating] = useState(false)
    const [deleting, setDeleting] = useState(null)
    const [searching, setSearching] = useState(false)
    const [headers, setHeaders] = useState({})
    const [show, setShow] = useState(false)
    const [loading, setLoading] = useState(false)
    const [pollyUrl, setPollyUrl] = useState(null);


    const history = useHistory();

    const { getSession } = useContext(AccountContext);

    useEffect(() => {
        getSession().then(async ({ headers }) => {
            const curHeaders = await headers
            setHeaders(curHeaders)
            loadNotes(curHeaders)
        }, (e) => {
            console.log(e)
            console.log("redirecting to login")
            history.push('/Login');
        })
    }, [])


    const getAPI = async (path, method, json = false, body = "", custHeader = headers) => {
        const url = Config.ApiURL
        let req
        switch (method) {
            case 'post':
                req = new Request(url + path, { method: method, headers: custHeader, body: body })
                break
            default:
                req = new Request(url + path, { method: method, headers: custHeader })
        }

        return await fetch(req)
            .then(response => {
                if (!response.ok) {
                    throw new Error(response.statusText);
                }
                return response
            })
            .then(response => {
                setSearching(false)
                return json ? response.json() : response.text()
            })
            .catch(response => {
                console.log(response)
                props.setError({
                    "message": "The API call returned an Error."
                })
                if (response == 'Unauthorized') {
                    history.push("/Login")
                }
                setShow(false)
                setPollyUrl(null)
                setLoading(false)
                setUpdating(false)
                setDeleting(null)
                setSearching(false)
            })
    }

    const loadNotes = async (custHeaders = headers) => {
        const apiRet = await getAPI("", 'get', true, "", custHeaders)
        setNotes(apiRet)
        setSelNote({ "NoteId": "" })
        setNotesLoaded(true);
        setUpdating(false)
        setDeleting(null)
    }

    const noteFunctions = {
        "handleSelectNote": (n) => {
            if (selNote.NoteId === n || n === undefined) {
                setSelNote({ "NoteId": "" })
            } else {
                setSelNote(notes.find(note => note.NoteId === n))
            }
        },
        "updateNote": async (n, newText) => {
            setUpdating(true)
            const newNote = { "NoteId": parseInt(n.NoteId), "Note": newText }
            await getAPI("", 'post', false, JSON.stringify(newNote))
            loadNotes()
        },
        "addNote": async (n) => {
            setUpdating(true)
            let tempArr = []
            notes.map(item => tempArr.push(parseInt(item.NoteId)))
            tempArr = tempArr.sort((a, b) => b - a)
            const newNum = tempArr[0] + 1

            let newNote = {
                "NoteId": newNum,
                "Note": n
            }

            await getAPI("", 'post', false, JSON.stringify(newNote))
            loadNotes()
        },
        "speakNote": async (id, voice) => {
            setShow(true)
            setPollyUrl(null)
            setPollyUrl(await getAPI('/' + id, 'post', true, JSON.stringify({ "VoiceId": voice })))
        },
        "deleteNote": async (id) => {
            setDeleting(id);
            await getAPI('/' + id, 'delete')
            loadNotes()
        },
        "filterNotes": async (q) => {
            console.log("filter with API:" + q)
            setSearching(true)
            setNotes(await getAPI('/search?text=' + q, 'get', true))

        }
    }

    return (
        <div>
            <PollyWindow show={show} setShow={setShow} loading={loading} url={pollyUrl} />
            {(notesLoaded) ? <NoteDisplay searching={searching} deleting={deleting} notes={notes} noteFunctions={noteFunctions} selNote={selNote} /> : <LoadingSpinner />}
            <NoteEntry updating={updating} selNote={selNote} noteFunctions={noteFunctions} />
        </div>
    );
}

export default Notes;