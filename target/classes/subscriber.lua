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

if #arg ~= 7 then
  print("ERROR\n")
  print("Command line arguments required for " .. file_path .. " are described below\n")
  print("<server_address> <server_port> <number_messages> <payload_size> <wait_per_message> <topic> <client_name>\n")
  -- it could be os.exit(1) to terminate the host program
  return
end

local server_address = arg[1]
local server_port = arg[2]
local number_messages = tonumber( arg[3] )
local payload_size = arg[4]
local wait_per_message = tonumber(arg[5])
local topic = arg[6]
local client_name = arg[7]

function callback(topic, message)
    -- print("Received: " .. topic .. ": " .. message)
end

mqtt_client = MQTT.client.create(server_address, server_port, callback)
mqtt_client:connect(client_name)
mqtt_client:subscribe({topic})

print("created MQTT client", mqtt_client)

local error_message = nil

local index = 1

while (index < number_messages) do
    error_message = mqtt_client:handler()
    socket.sleep(wait_per_message)
    print("publishing message")
    -- TODO use string with payload size
    local random_num = math.random(1,64)
    print("publishing: ",random_num)
    mqtt_client:publish(topic, random_num)
    index = index + 1
end

