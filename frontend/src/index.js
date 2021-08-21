import React from 'react';
import ReactDOM from 'react-dom';
import './index.css';
import { Grid, Cell } from "styled-css-grid";
import lamejs from 'lamejs';
import "react-loader-spinner/dist/loader/css/react-spinner-loader.css";
import Loader from "react-loader-spinner";
import VideoSection from './VideoSection'
import reportWebVitals from './reportWebVitals';

// If you want to start measuring performance in your app, pass a function
// to log results (for example: reportWebVitals(console.log))
// or send to an analytics endpoint. Learn more: https://bit.ly/CRA-vitals
reportWebVitals();

function download(blob, name) {
  const link = document.createElement('a');
  link.href = URL.createObjectURL(blob);
  link.download = name;
  link.target = '_blank';
  link.setAttribute("type", "hidden");
  document.body.appendChild(link); // needed for firefox (?)
  link.click();
  link.remove();
}

let getAudioContext = () => new (window.AudioContext || window.webkitAudioContext)();

function slice(decodedAudio, start, end, bitrate) {
  let chans = decodedAudio.numberOfChannels;
  if (chans !== 1 && chans !== 2) {
    // TODO can it handle other cases?
        throw new Error(`Can only slice audio with 1 or 2 channels but was ${chans}`);
  }

  //Compute start and end values in seconds
  let computedStart = decodedAudio.length * start / decodedAudio.duration;
  let computedEnd = decodedAudio.length * end / decodedAudio.duration;
  let audioContext = getAudioContext();
  const newBuffer = audioContext.createBuffer(
    chans,
    computedEnd - computedStart,
    decodedAudio.sampleRate)

  // Copy from old buffer to new with the right slice.
  // At this point, the audio has been cut
  for (var i = 0; i < chans; i++) {
    newBuffer.copyToChannel(decodedAudio.getChannelData(i).slice(computedStart, computedEnd), i)
  }

  const channelData = [];
  for (let i = 0; i < chans; ++i) {
    channelData.push(newBuffer.getChannelData(i));
  }

  var encoder = new lamejs.Mp3Encoder(chans, decodedAudio.sampleRate, bitrate);
  var mp3Data = [];
  if (decodedAudio.numberOfChannels === 1) {
    console.log('encoding mono');
    var mp3Tmp = encoder.encodeBuffer(channelData);
    mp3Data.push(mp3Tmp);
    mp3Tmp = encoder.flush();
    mp3Data.push(mp3Tmp);
  }
  else {
    // TODO audio is correct length but has no apparent sound
    // wondering if format is just weird, like it came in as mp4
    // and lameJS might want it as mp3 already
    console.log('encoding stereo');
    const sampleBlockSize = 1152; //can be anything but make it a multiple of 576 to make encoders life easier
    let leftChan = channelData[0];
    let rightChan = channelData[1];

    // scale audio encoding (otherwise it's quiet)
    var left = new Float32Array(leftChan.length);
    var right = new Float32Array(rightChan.length);
    for(i = 0; i < Math.min(left.length, right.length); i++) {
        left[i] = left[i] * 32767.5;
        right[i] = right[i] * 32767.5;
    }

    for (i = 0; i < left.length; i += sampleBlockSize) {
      let leftChunk = left.subarray(i, i + sampleBlockSize);
      let rightChunk = right.subarray(i, i + sampleBlockSize);
      let mp3buf = encoder.encodeBuffer(leftChunk, rightChunk);
      if (mp3buf.length > 0) {
        mp3Data.push(mp3buf);
      }
    }

    let mp3buf = encoder.flush();   //finish writing mp3
    if (mp3buf.length > 0) {
        mp3Data.push(mp3buf);
    }
  }

  console.log(`mp3Data: ${mp3Data}`);
  return new Blob(mp3Data); //, {type: 'audio/mp3'});
}

function getVideoId(text) {
  const youtubeRegex = /^(?:https?:\/\/)?(?:www\.)?(?:youtu\.be\/|youtube\.com\/(?:embed\/|v\/|watch\?v=|watch\?.+&v=))((\w|-){11})(?:\S+)?$/;
  if (!text.match(youtubeRegex))
  {
    return null;
  }
  if (!text.startsWith('http://') && !text.startsWith('https://'))
  {
    text = 'https://' + text;
  }
  const url = new URL(text);
  const urlParams = new URLSearchParams(url.search);
  return urlParams.get('v') ?? url.pathname.substring(1);
}

class StartForm extends React.Component {
  constructor(props) {
    super(props);
    this.state = {url: '', sections: []}; 
    this.handleVideoUrlInputChange = this.handleVideoUrlInputChange.bind(this);
    this.handleSubmit = this.handleSubmit.bind(this);
    this.handleDownloadEntireVideo = this.handleDownloadEntireVideo.bind(this);
    this.handleDownload = this.handleDownload.bind(this);
    this.onSectionSelectedChange = this.onSectionSelectedChange.bind(this);
    this.onSectionNameChange = this.onSectionNameChange.bind(this);
    this.onAllSectionsSelectedChange = this.onAllSectionsSelectedChange.bind(this);
    this.nullIfNoSections = this.nullIfNoSections.bind(this);
    this.downloadSpinner = this.downloadSpinner.bind(this);
    this.request = this.request.bind(this);
  }

  handleVideoUrlInputChange(event) {
    let url = event.target.value;
    let videoId = getVideoId(url);
    this.setState({
      url: url,
      videoId: videoId
    });
  }

  request(endpoint, responseHandler, requestParams = null) {
    let errorMsg = "";
    this.setState({
      errorMessage: errorMsg,
      downloading: true
    });
    fetch(`http://localhost:8080/${endpoint}`, requestParams)
      .then(response => {
        if (!response.ok) {
          return response.text().then(text => { throw Error(text); });
        }
        return response;
      })
      .then(response => responseHandler(response))
      .catch(error => {
        let msg = error.message;
        console.log(`Request to ${endpoint} failed: ${msg}`);
        errorMsg = `Error: ${msg}`;
      })
      .finally(() => {
        this.setState({
          downloading: false,
          errorMessage: errorMsg,
        });
      });
  }

  handleSubmit(event) {
    let fetchedVideoId = this.state.videoId;
    this.request(`sections/${fetchedVideoId}`, response => response.json().then(data => this.setState({
      videoInfo: {
        title: data.title,
        start: 0,
        end: data.length,
        selected: true
      },
      sections: data.sections.map(t => ({ ...t, selected: true})),
      fetchedVideoId: fetchedVideoId
    })));
    event.preventDefault();
  }

  handleDownloadEntireVideo(event) {
    let videoId = this.state.videoId;
    let videoTitle = this.state.videoInfo.title;
    this.request(
      `download/${videoId}`,
      response => response.blob().then(blob => download(blob, `${videoTitle}.mp3`)));
    event.preventDefault();
  }

  handleDownload(event) {
    let videoId = this.state.videoId;
    // TODO probably have slice take the full audio blob and decode it
    let audioCtx = getAudioContext();
    this.request(
      `download/${videoId}`,
      response => response.blob()
        .then(blob => blob.arrayBuffer().then(arrayBuffer => {
          audioCtx.decodeAudioData(arrayBuffer, buffer => {
            let slicedBlob = slice(buffer, 0, 2, 192);
            download(slicedBlob, 'slicedBlob.mp3');
          });
        })
    ));
  }

  onSectionSelectedChange(event) {
    let sections = this.state.sections;
    let index = event.target.getAttribute("index");
    sections[index].selected = event.target.checked;
    this.setState({sections: sections});
  }

  onSectionNameChange(event) {
    let sections = this.state.sections;
    let index = event.target.getAttribute("index");
    sections[index].name = event.target.value;
    this.setState({sections: sections});
  }
  
  onAllSectionsSelectedChange(event) {
    let sections = this.state.sections;
    sections.forEach(t => t.selected = event.target.checked);
    this.setState({sections: sections});
  }
  
  nullIfNoSections(element) {
    return this.state.sections.length > 0
      ? element
      : null;
  }

  downloadSpinner() {
    if (this.state.downloading) {
      return (
        <Loader
          type="Watch"
          color="#2ba805"
          height={150}
          width={150}/>);
    }
  }

  render() {
    let urlInput = (<input id="urlInput" type="text" onChange={this.handleVideoUrlInputChange}/>);
    let submitBtn = (
    <button
        id="submitBtn"
        type="submit"
        disabled={!this.state.videoId}
        // show glowing animation if valid video is entered and hasn't been fetched yet
        style={{animation: this.state.videoId && this.state.fetchedVideoId !== this.state.videoId
          ? 'glowing 1300ms infinite'
          : 'none'}}
        onClick={this.handleSubmit}>
        Submit
    </button>);
    let errorLabel = (<label>{this.state.errorMessage}</label>);
    let selectAllInput = this.nullIfNoSections(
      <input checked={this.state.sections.every(t => t.selected)}
             onChange={this.onAllSectionsSelectedChange}
             type="checkbox"
             name="changeAllSelection"
             id="changeAllSelection"
             disabled={this.state.downloading}/>);
    let selectAllInputLabel = this.nullIfNoSections(
      <label htmlFor="changeAllSelection">Select / unselect all sections</label>
    );
    let sectionsList = (
      <ul>
        {
          this.state.sections.map((section, index) => (
            <li
              style={{ listStyleType: "none" }} 
              key={index}>
              <VideoSection
                index={index}
                onSelectedChange={this.onSectionSelectedChange}
                onNameChange={this.onSectionNameChange}
                isChecked={section.selected}
                value={section.name}
                startTime={section.start}
                endTime={section.end}
                style={{width: '800px'}}
                videoId={this.state.fetchedVideoId}
                disabled={this.state.downloading}
              />
            </li>
          ))
        }
        </ul>
    );
    let downloadBtn = (
      this.nullIfNoSections(
        <button
          type="button"
          disabled={this.state.downloading}
          onClick={this.handleDownload}>
          Download selected sections
      </button>));
    let videoTitleLabel = null;
    let downloadEntireVideoBtn = null;
    if (this.state.fetchedVideoId != null) {
      videoTitleLabel = (
      <div>
        <label>Video:     </label>
        <label style={{fontStyle: 'italic'}}>
          {this.state.videoInfo.title}
        </label>
      </div>
      );
      downloadEntireVideoBtn = (
        <button
          type="button"
          disabled={this.state.downloading}
          onClick={this.handleDownloadEntireVideo}>
          Download entire video
        </button>
      );
    }
    return (
    <form>
      <Grid columns={"1fr"} rows={"1fr"}>
        <Cell center>
          <h1 style={{fontSize: '50px', fontFamily: 'Garamond'}}>
            Youtube Mixtape Downloader
          </h1>
          <hr style={{margin: "0px 0px 20px 0px"}}/>
        </Cell>
        <Cell center>
          <label style={{fontSize: '30px'}}>Enter a YouTube link:</label>
        </Cell>
        <Cell center>
          {urlInput}
        </Cell>
        <Cell center>
          {submitBtn}
        </Cell>
        <Cell center>
          {errorLabel}
          {this.downloadSpinner()}
        </Cell>
        <Cell center>{videoTitleLabel}</Cell>
        <Cell center>{downloadEntireVideoBtn}</Cell>
        <Cell center>{downloadBtn}</Cell>
        <Cell center>
          {selectAllInput}
          {selectAllInputLabel}
        </Cell>
        <Cell center>{sectionsList}</Cell>
      </Grid>
    </form>
    );
  }
}

ReactDOM.render(
  <React.StrictMode>
    <StartForm />
  </React.StrictMode>,
  document.getElementById('root')
);

// TODO
// - styling
// - client validation and/or cleaning of filenames?
// - allow downloading audio or video; format and quality selection
// - Button alongside each section to download separately
// - integration tests for client (selenium ?)
// - more integration tests for server