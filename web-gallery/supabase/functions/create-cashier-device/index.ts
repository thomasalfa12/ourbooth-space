import { serve } from "https://deno.land/std@0.168.0/http/server.ts"
import { createClient } from 'https://esm.sh/@supabase/supabase-js@2'

const corsHeaders = {
  'Access-Control-Allow-Origin': '*',
  'Access-Control-Allow-Headers': 'authorization, x-client-info, apikey, content-type',
}

serve(async (req) => {
  // Handle CORS preflight requests
  if (req.method === 'OPTIONS') {
    return new Response('ok', { headers: corsHeaders })
  }

  try {
    // Create Supabase admin client
    const supabaseUrl = Deno.env.get('SUPABASE_URL')!
    const supabaseServiceKey = Deno.env.get('SUPABASE_SERVICE_ROLE_KEY')!
    const supabaseAdmin = createClient(supabaseUrl, supabaseServiceKey)

    // Get the auth token from the request
    const authHeader = req.headers.get('Authorization')
    if (!authHeader) {
      throw new Error('No authorization header')
    }

    // Verify the user is an ADMIN
    const { data: { user }, error: userAuthError } = await supabaseAdmin.auth.getUser(
      authHeader.replace('Bearer ', '')
    )

    if (userAuthError || !user) {
      throw new Error('Invalid authentication')
    }

    // Check user role from profiles table
    const { data: profile, error: profileError } = await supabaseAdmin
      .from('profiles')
      .select('role')
      .eq('id', user.id)
      .single()

    if (profileError || !profile || profile.role !== 'ADMIN') {
      throw new Error('Access denied. Admin role required.')
    }

    // Parse request body
    const { deviceName, deviceType, email, password } = await req.json()

    if (!deviceName || !deviceType || !email || !password) {
      throw new Error('Missing required fields: deviceName, deviceType, email, password')
    }

    // Validate deviceType
    if (!['RENTAL', 'VENDING'].includes(deviceType)) {
      throw new Error('deviceType must be either RENTAL or VENDING')
    }

    // 1. CREATE DEVICE (Generate Username/PIN)
    const randomSuffix = Math.floor(1000 + Math.random() * 9000)
    const generatedUsername = `booth_${randomSuffix}`
    const generatedPin = Math.floor(100000 + Math.random() * 900000).toString()

    const { data: device, error: devError } = await supabaseAdmin
      .from('devices')
      .insert({
        name: deviceName,
        type: deviceType,
        username: generatedUsername,
        pin_code: generatedPin,
        status: 'ACTIVE'
      })
      .select()
      .single()

    if (devError) throw devError

    // 2. CREATE AUTH USER (Cashier)
    const { data: userResult, error: createAuthError } = await supabaseAdmin.auth.admin.createUser({
      email: email,
      password: password,
      email_confirm: true, // Langsung confirm biar bisa login
      user_metadata: {
        role: 'CASHIER'
      }
    })

    if (createAuthError) throw createAuthError

    // 3. LINKING (Update Profile User tadi dengan Device ID)
    // Trigger otomatis kita sebelumnya sudah membuat row profile, kita tinggap update.
    const { error: linkError } = await supabaseAdmin
      .from('profiles')
      .update({ 
        role: 'CASHIER',
        assigned_device_id: device.id // INI KUNCINYA!
      })
      .eq('id', userResult.user.id)

    if (linkError) throw linkError

    return new Response(
      JSON.stringify({ 
        success: true, 
        device,
        user: {
          id: userResult.user.id,
          email: userResult.user.email,
          role: 'CASHIER'
        },
        credentials: {
          username: generatedUsername,
          pin: generatedPin
        }
      }),
      { 
        headers: { 
          'Content-Type': 'application/json',
          ...corsHeaders
        } 
      }
    )

  } catch (error) {
    console.error('Error in create-cashier-device function:', error)
    
    const errorMessage = error instanceof Error ? error.message : 'An unexpected error occurred'
    
    return new Response(
      JSON.stringify({
        success: false,
        error: errorMessage
      }),
      {
        status: 400,
        headers: {
          'Content-Type': 'application/json',
          ...corsHeaders
        }
      }
    )
  }
})