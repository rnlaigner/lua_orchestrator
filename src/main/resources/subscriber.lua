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

local publishers = {}

--local client_info = {}

function callback(topic, message)

    local time = socket.gettime()

    local i, j = string.find(message," | ")
    local client_part = string.sub(message, 1, i)

    local i_, j_ = string.find(client_part,":")
    local player_name = string.sub(client_part, j_+2, string.len(client_part)-1)

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
mqtt_client:subscribe({topic})

--print("created MQTT client")

local error_message = nil

local index = 1

while (index < number_messages) do
    error_message = mqtt_client:handler()
    socket.sleep(wait_per_message)
    --print("publishing message")
    -- TODO use string with payload size
    --print("publishing message: ",index)
    --local time_sent = socket.gettime()
    --table.insert(client_info,index,time_sent)
    mqtt_client:publish(topic, "client: "..client_name.." | message: "..index)
    index = index + 1
end

-- TODO enquanto houver msgs a receber, devo chamar o handler

--mqtt_client:destroy()

--  return publishers
--io.write(publishers)


--for idx, time in ipairs(client_info) do
--    print(idx)
--    print(time)
--end


-- write time elapsed for each client message

print(client_name)

for idx, publisher_tbl in pairs(publishers) do
    for i, msg in ipairs(publisher_tbl) do
        print(idx)
        print(i)
        print(msg)
    end
end

--io.stdout.write(publishers)

--io.stdout.flush()

--io.stderr:write("Third stderr TEST\n")

--io.stderr.flush()

--print("finished")

