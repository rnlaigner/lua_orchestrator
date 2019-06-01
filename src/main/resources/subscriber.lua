------------------------------------------------------------------------------------------------------------
-- Trabalho 3 - MQTT
-- Author: Rodrigo Laigner
------------------------------------------------------------------------------------------------------------

-- imports

-- import should be global because mqtt_library makes use of
socket = require("socket")

local MQTT = require("mqtt_library")

-- args parser
local file_path = arg[0]

if #arg ~= 8 then
  print("ERROR\n")
  print("Command line arguments required for " .. file_path .. " are described below\n")
  print("<server_address> <server_port> <number_clients> <number_messages> <payload_size> <wait_per_message> <topic> <client_name>\n")
  -- it could be os.exit(1) to terminate the host program
  return
end

local server_address = arg[1]
local server_port = arg[2]
local number_clients = tonumber( arg[3] )
local number_messages = tonumber( arg[4] )
local payload_size = arg[5]
local wait_per_message = tonumber( arg[6] )
local topic = arg[7]
local client_name = arg[8]

local publishers = {}

local can_publish = false

function is_there_remaining_messages()
    for idx, tbl in pairs(publishers) do
        if ( #tbl < number_messages ) then
            return true
        end
    end
    return false
end

function callback(topic, message)

    if( topic == "$SYS/broker/clients/connected" ) then
        if tonumber( message ) == number_clients then
            can_publish = true
        end
        return
    end

    local time = socket.gettime()

    local i, j = string.find(message," | ")
    local client_part = string.sub(message, 1, i)

    local i_, j_ = string.find(client_part,":")
    local player_name = string.sub(client_part, j_+2, string.len(client_part)-1)

    if (player_name == client_name) then
        return
    end

    if ( publishers[player_name] ) then
        local tbl = publishers[player_name]
        table.insert( tbl, time )
    else
        local tbl = {}
        table.insert( tbl, time )
        publishers[player_name] = tbl
    end

end

mqtt_client = MQTT.client.create(server_address, server_port, callback)
mqtt_client:connect(client_name)
mqtt_client:subscribe({topic,"$SYS/broker/clients/connected"})

local error_message = nil

local msg_index = 1

while not can_publish do
    mqtt_client:handler()
end

local start_time = socket.gettime()

while (msg_index <= number_messages) do
    error_message = mqtt_client:handler()
    -- TODO use string with payload size
    mqtt_client:publish(topic, "client: "..client_name.." | message: ".. tostring(msg_index))
    msg_index = msg_index + 1
end

--print("finished sending")

-- enquanto houver msgs a receber, devo chamar o handler
while is_there_remaining_messages() do
    error_message = mqtt_client:handler()

    socket.sleep(0.1)  -- seconds

    --print("waiting for messages...")
end

mqtt_client:unsubscribe({topic,"$SYS/broker/clients/connected"})
mqtt_client:destroy()

local finish_time = socket.gettime()

-- write client name
print(client_name)
print(start_time)
print(finish_time)
print(finish_time - start_time)

--os.exit(1)