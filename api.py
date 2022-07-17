from fastapi import FastAPI

app = FastAPI()

requests = {0: {"open": False, "state": "arrived", "driver": "Raviish", "dest": [77.635,12.9125], "driverPos": [None, None], "distance": None, "duration": None, "clientPos": [None, None], "driverBearing": 0.0, "totalDistance": None}}

@app.get('/request/{lng}/{lat}/{clientLng}/{clientLat}')
def home(lng, lat, clientLng, clientLat):
    lat = float(lat)
    lng = float(lng)
    clientLat = float(clientLat)
    clientLng = float(clientLng)
    index = len(requests) + 1
    requests[index] = {"open": True, "state": "notClaimed", "driver": None, "dest": [lng, lat], "driverPos": [None, None], "distance": None, "duration": None, "clientPos": [clientLng, clientLat], "driverBearing": 0.0, "totalDistance": None}
    return index

@app.get('/get_request/{request_id}/{accept}')
def getRequest(request_id, accept):
    accept = int(accept)
    if accept == 1 and requests[int(request_id)]["state"] == "waitingForClient":
        requests[int(request_id)].update({"state": "toDestination"})

    return requests.get(int(request_id))

@app.get('/delete_request/{request_id}')
def deleteRequest(request_id):
    if request_id == -1:
        return
    
    try:
        requests.pop(int(request_id))
    except:
        pass

@app.get('/get_requests')
def getRequests():
    return {k:v for (k,v) in requests.items() if v["open"]}


@app.get('/claim_request/{request_id}/{distance}/{duration}/{lng}/{lat}/{driver}/{state}/{bearing}/{totalDistance}')
def claim_request(request_id, distance, duration, lng, lat, driver, state, bearing, totalDistance):
    request_id = int(request_id)
    
    if requests[request_id]["driver"] == None:
        requests[request_id].update({"open": False, "state": "toClient", "driver": driver, "driverPos": [float(lng), float(lat)], "distance": float(distance), "duration": float(duration), "driverBearing": float(bearing), "totalDistance": float(totalDistance)})
    elif requests[request_id]["driver"] == driver:


        if state == "arrived":
            requests[request_id].update({"state": state, "distance": 0.0, "duration": 0.0, "driverBearing": 0.0})
            return requests[request_id]
        if state == "waitingForClient" and requests[request_id]["state"] == "toDestination":
            state = "toDestination"
        if state == "waitingForClient" and requests[request_id]["state"] == "toClient":
            requests[request_id].update({"state": state})
        else:
            requests[request_id].update({"state": state, "driverPos": [float(lng), float(lat)], "distance": float(distance), "duration": float(duration), "driverBearing":float(bearing), "totalDistance": float(totalDistance)})
    #f
    if requests[request_id]["driver"] == driver:
        return requests[request_id]
    else:
        return {}
